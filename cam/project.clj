(defproject cam "0.1.0-SNAPSHOT"
  :description "FIXME"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.cli "0.4.2"]
                 [compojure "1.6.1"]
                 [ring/ring-core "1.7.1"]
                 [ring/ring-devel "1.7.1"]
                 [http-kit "2.3.0"]
                 [cheshire "5.9.0"]
                 [clj-time "0.15.2"]
                 [net.bramp.ffmpeg/ffmpeg "0.6.2"]
                 [javax.servlet/servlet-api "2.5"]]
  :main cam.main
  :profiles
  {:uberjar {:aot :all}
   :production {}
   :dev {:dependencies [[ring/ring-mock "0.4.0"]]}})
