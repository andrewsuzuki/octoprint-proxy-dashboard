(ns cam.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cam.config :as config]
            [cam.snapshot :as snapshot]
            [cam.utils :refer [json-response]]))

(defn snapshot-handler
  "Take new snapshot from webcam and return as data uri image"
  [req]
  (snapshot/camera->data-uri (config/get-config :device)))

(defroutes app-routes
  (GET "/" [] "cam snapshot service")
  (GET "/snapshot" [] snapshot-handler)
  (route/not-found "Not Found"))

; TODO generic error handler

(def app
  (-> (routes app-routes)
      (handler/site)))
