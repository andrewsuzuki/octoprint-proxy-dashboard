(ns cam.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [cam.config :as config]
            [ring.util.response :refer [response status]]
            [cam.snapshot :as snapshot]))

(defn snapshot-handler
  "Take new snapshot from webcam and return as data uri image"
  [req]
  (let [device (config/get-config :device)]
    (try
      (-> device
          (snapshot/camera->data-uri)
          (response))
      (catch Exception e
        (println "Camera snapshot exception: " (.getMessage e))
        ; simple server error response
        (-> (response "Camera error")
            (status 500))))))

(defroutes app-routes
  (GET "/" [] (response "Camera snapshot service"))
  (GET "/snapshot" [] snapshot-handler)
  (route/not-found (-> (response "Not Found")
                       (status 404))))

(def app
  (-> (routes app-routes)
      (handler/site)))
