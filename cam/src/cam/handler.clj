(ns cam.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cam.utils :refer [json-response]]))

(defn snapshot-handler
  "Take new snapshot from webcam and return as data uri image"
  [req]
  (json-response {:foo "bar"}))

(defroutes app-routes
  (GET "/" [] "cam snapshot service")
  (GET "/snapshot" [] snapshot-handler)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (handler/site)))
