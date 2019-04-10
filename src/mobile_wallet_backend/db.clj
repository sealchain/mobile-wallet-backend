(ns mobile-wallet-backend.db
  (:require
   [mount.core :as mount]
   [clojure.java.jdbc :as j]
   [taoensso.timbre :as log]
   [hikari-cp.core :refer [make-datasource
                           close-datasource]]
   [mobile-wallet-backend.global :as g])
  (:import [org.postgresql.util PGobject]))

(mount/defstate datasource
  :start (let [options (merge
                        {:read-only          false
                         :connection-timeout 30000
                         :validation-timeout 5000
                         :idle-timeout       600000
                         :max-lifetime       1800000
                         :minimum-idle       10
                         :maximum-pool-size  10
                         :pool-name          "db-pool"
                         :adapter            "postgresql"}
                        (:db @g/config))
               ds (make-datasource options)]
           (log/info (str "Conn pool ready. " (name (:env @g/config))))
           ds)
  :stop (do
          (close-datasource datasource)
          (log/info "Conn pool closed.")))

(defn query [stmt]
  (j/with-db-connection [conn {:datasource datasource}]
    (j/query conn stmt)))

(defn update-opt [table set-map where-clause]
  (j/with-db-connection [conn {:datasource datasource}]
    (j/update! conn table set-map where-clause)))

(defn pgobject->vec
  [obj]
  (if (instance? org.postgresql.jdbc.PgArray obj)
    (vec (.getArray obj))
    obj))

(defn serialize-pg-map
  [mp]
  (into {} (for [[k v] mp] [k (pgobject->vec v)])))
