(ns api.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [org.httpkit.server :refer [with-channel on-close send! on-receive]]
            [api.broadcast :as broadcast]
            [api.cam :as cam]
            [api.utils :refer [json-response]]))

(defn hydrate-handler
  "hydrate new client with current state"
  [req]
  ;; TODO include printer state
  (let [m {:cams @cam/cams
           :printers []}]
    (json-response m)))

(defn subscribe-handler
  "stream (websocket or http long-poll) updates to client"
  [req]
  (with-channel req ch
    (broadcast/add broadcast/channel-store ch {})
    (on-close ch (fn [status]
                   (broadcast/end broadcast/channel-store ch)))
    (on-receive ch (fn [data]
                     ; do nothing
                     nil))))

(defroutes app-routes
  (GET "/" [] "octoprint api proxy")
  (GET "/hydrate" [] hydrate-handler)
  (GET "/subscribe" [] subscribe-handler)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (handler/site)))
