(defproject mobile_wallet_backend "0.1.0-SNAPSHOT"

  :description "FIXME: write description"

  :url "http://example.com/FIXME"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/java.jdbc "0.7.8"]
                 [org.postgresql/postgresql "42.2.5"]
                 [compojure "1.6.1"]
                 [ring "1.7.1"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-defaults "0.3.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.taoensso/tufte "2.0.1"]
                 [byte-streams "0.2.4"]
                 [aleph "0.4.6"]
                 [org.clojure/tools.nrepl "0.2.13"]
                 [ring-json-response "0.2.0"]
                 [manifold "0.1.8"]
                 [clj-time "0.15.0"]
                 [mount "0.1.13"]
                 [hikari-cp "2.6.0"]]

  :plugins [[lein-ring "0.12.4"]
            [cider/cider-nrepl "0.18.0"]]
  
  :main ^:skip-aot mobile-wallet-backend.core

  :target-path "target/%s"

  :profiles {:uberjar {:aot :all}})
