(ns mobile-wallet-backend.global
  (:require [clojure.string :as str]))

(def config (atom {}))

(def pid
  (-> (java.lang.management.ManagementFactory/getRuntimeMXBean)
      (.getName)
      (str/split #"@")
      first))
