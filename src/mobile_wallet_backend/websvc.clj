(ns mobile-wallet-backend.websvc
  (:require
    [aleph.http :as http]
    [manifold.stream :as s]
    [manifold.deferred :as d]
    [manifold.bus :as bus]
    [mount.core :as mount]
    [taoensso.timbre :as log]
    [mobile-wallet-backend.global :as g]
    [compojure.core :as compojure :refer [GET POST]]
    [ring.middleware.params :as params]
    [ring.middleware.reload :refer [wrap-reload]]
    [ring.middleware.multipart-params :as mp]
    [ring.middleware.keyword-params :as kw]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [ring.middleware.json :refer [wrap-json-body]]
    [compojure.route :as route]
    [clojure.string :as str]
    [ring.util.response :refer [response]]
    [ring.util.json-response :refer [json-response]]
    [mobile-wallet-backend.db :as db]
    [byte-streams :as bs]
    [cheshire.core :as json])
  (:use
    [ring.util.response :only (response content-type)]))

(defn hello-world-handler
  "A basic Ring handler which immediately returns 'hello world'"
  [req]
  {:status  200
   :headers {"content-type" "text/plain"}
   :body    "hello world!"})

(defn hello-json-handler
  [req]
  (json-response {:foo "bar"}))

(defn mk-array
  [addresses]
  (str "array['" (str/join "','" addresses) "']"))

;; 允许自定义时间格式
(defn json-response-opt
  [data opt-map]
  (-> (response (json/generate-string data opt-map))
      (content-type "application/json")))

(defn filter-used-addresses
  [req]
  (log/info "filter-used-addresses")
  (log/info req)
  (let [{:keys [addresses]} (:body req)
        stmt (format "SELECT DISTINCT address FROM \"tx_addresses\" WHERE address = ANY(%s)"
                     (mk-array addresses))
        rows (db/query stmt)]
    (log/info [addresses stmt rows])
    (json-response (map :address rows))))

(defn utxo-for-addresses
  [req]
  (log/info "utxo-for-addresses")
  (log/info req)
  (let [{:keys [addresses]} (:body req)
        stmt (format " SELECT * FROM \"utxos\" WHERE receiver = ANY(%s)"
                     (mk-array addresses))
        rows (db/query stmt)]
    (log/info [addresses stmt rows])
    (log/info (count rows))
    (json-response rows)))

(defn utxo-sum-for-addresses
  [req]
  (log/info "utxo-sum-for-addresses")
  (log/info req)
  (let [{:keys [addresses]} (:body req)
        stmt (format "SELECT currency, SUM(amount) FROM \"utxos\" WHERE receiver = ANY(%s) GROUP BY currency"
                     (mk-array addresses))
        rows (db/query stmt)]
    (log/info [addresses stmt rows])
    (json-response rows)))

(defn txs-history
  [req]
  (log/info "txs-history")
  (log/info req)
  (let [{:keys [addresses dateFrom limit]} (:body req)
        stmt (format "SELECT * FROM \"txs\"
                     LEFT JOIN (SELECT * from \"bestblock\" LIMIT 1) f ON true
                     WHERE hash = ANY (SELECT tx_hash FROM \"tx_addresses\" WHERE address = ANY (%s))
                     AND last_update < '%s'
                     ORDER BY last_update DESC
                     LIMIT %d"
                     (mk-array addresses) dateFrom limit)
        rows (db/query stmt)]
    (log/info stmt)
    (log/info [addresses dateFrom limit])
    (json-response (map db/serialize-pg-map rows))))

(def non-ws-request
  {:status 400
   :headers {"content-type" "application/text"}
   :body "Expected a websocket request."})

(def ebs (bus/event-bus))

(defn ws-handle
  [req source]
  (d/let-flow [conn (d/catch (http/websocket-connection req)
                         (fn [e]
                           (log/error e)))]
     (if conn
       ;; (s/connect (or source conn) conn) ;; if source is nil, just echo
       (s/put-all! conn (map :receiver source))
       non-ws-request)))

;; no need to distinguish clients, use one stream for each ws connection:
(defn wrap-ws-handler
  ([req]
   (ws-handle req nil))
  ([req source]
   (ws-handle req source)))

(def echo-ws wrap-ws-handler)

(defn txs-unspent-addresses
  [req]
  (log/info req)
  (let [stmt (format "SELECT DISTINCT utxos.receiver FROM utxos")
        rows (db/query stmt)]
    (wrap-ws-handler req rows)))

(defn signed-txs
  [req]
  (log/info "signed-txs")
  (log/info req)
  (let [req-body (:body req)
        signedTx (:signedTx req-body)]
    (let [res @(http/post (:chain-importer-url @g/config)
                {:form-params {:signedTx signedTx}
                 :content-type :json})]
     (log/info "response" res)
     (let [body (bs/to-string (:body res))]
       (log/info "body" body)
       (response body)))))

;; 查看版本信息
(defn version-info
  [req]
  (let [stmt "SELECT * FROM version_info"
        rows (db/query stmt)]
    (json-response (first (map db/serialize-pg-map rows)))))

;; 更新版本信息
(defn version-update
  [req]
  (let [{:keys [version apkUrl describe force]} (:body req)
        rs (db/update-opt
             :version_info
             {:version version, :apk_url apkUrl, :describe describe, :force force}
             ["1=1"])]
    (json-response (first rs))))

(def http-handler
  (-> (compojure/routes
        (GET "/hello" [] hello-world-handler)
        (GET "/hello-json" [] hello-json-handler)
        (POST "/addresses/filter-used" [] filter-used-addresses)
        (POST "/txs/utxo-for-addresses" [] utxo-for-addresses)
        (POST "/txs/utxo-sum-for-addresses" [] utxo-sum-for-addresses)
        (POST "/txs/history" [] txs-history)
        (POST "/txs/signed" [] signed-txs)
        (GET "/echo-ws" [] echo-ws)
        (GET "/txs/unspent-addresses" [] txs-unspent-addresses)
        (GET "/version" [] version-info)
        (POST "/version/update" [] version-update))
       ;; wrap-reload
       kw/wrap-keyword-params
       params/wrap-params
       mp/wrap-multipart-params
       (wrap-json-body {:keywords? true :bigdecimals? true})
       ))

(mount/defstate http-server
  :start (let [port (:http-port @g/config)
               server (http/start-server http-handler {:port port})]
           (log/info (str "http server started on port: " port "."))
           server)
  :stop (do
          (.close http-server)
          (log/info "http server stopped.")))
