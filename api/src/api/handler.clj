(ns api.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [org.httpkit.server :refer [with-channel on-close send! on-receive]]
            [api.broadcast :as broadcast]))

(defn subscribe-handler [req]
  (with-channel req ch
    (broadcast/add broadcast/channel-store ch {})
    (on-close ch (fn [status]
                   (broadcast/end broadcast/channel-store ch)))
    (on-receive ch (fn [data]
                     (send! ch data)))))

(defroutes app-routes
  (GET "/" [] "octoprint api proxy")
  (GET "/subscribe" [] subscribe-handler)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (handler/site)))
