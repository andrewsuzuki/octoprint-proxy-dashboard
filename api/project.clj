(defproject api "0.1.0-SNAPSHOT"
  :description "FIXME"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [compojure "1.6.1"]
                 [ring/ring-core "1.7.1"]
                 [http-kit "2.3.0"]
                 [cheshire "5.9.0"]
                 [org.clojure/core.async "0.4.500"]
                 [clj-time "0.15.2"]
                 [aleph "0.4.6"]
                 [manifold "0.1.8"]
                 [lambdaisland/uri "1.1.0"]]
  :main api.main
  :profiles
  {:uberjar {:aot :all}
   :production {}
   :dev {:dependencies [[ring/ring-mock "0.4.0"]
                        [ring/ring-devel "1.7.1"]
                        [javax.servlet/servlet-api "2.5"]]}})
