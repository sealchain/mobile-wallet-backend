(ns mobile-wallet-backend.core
  (:require
   [mount.core :as mount]
   [taoensso.timbre :as log]
   [taoensso.timbre.appenders.3rd-party.rotor :as log0]
   [mobile-wallet-backend.global :as g]
   [mobile-wallet-backend.nrepl :refer [nrepl-server]]
   [mobile-wallet-backend.websvc :refer [http-server]]
   [mobile-wallet-backend.db :refer [datasource]])
  (:gen-class))

(defn- setup-log []
  (log/merge-config!
   {:level :warn
    :appenders
    {:rotor
     (log0/rotor-appender
      {:path     "backend.log"
       :max-size 1024000})}})
  (log/handle-uncaught-jvm-exceptions!))

(defn load-config []
  (swap! g/config merge (load-file "conf/config.clj")))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (setup-log)
  (load-config)
  (mount/start #'nrepl-server)
  (mount/start #'http-server)
  (mount/start #'datasource)
  (log/info "server started! Running on PID: " g/pid "."))
