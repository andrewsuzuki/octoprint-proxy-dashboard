(ns api.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [org.httpkit.server :refer [with-channel on-close send! on-receive]]
            [api.broadcast :as broadcast]
            [api.cam :as cam]
            [api.octoprint :as octoprint]
            [api.utils :refer [json-response]]))

(defn hydrate-handler
  "hydrate new client with current state"
  [req]
  (letfn [(stringify-timestamps [els]
            (map broadcast/stringify-timestamp-m els))]
    (let [m {:cams (stringify-timestamps @cam/cams)
             :printers (-> @octoprint/printers
                           vals
                           (or [])
                           (stringify-timestamps))}]
      (json-response m))))

(defn subscribe-handler
  "stream (websocket or http long-poll) updates to client"
  [req]
  (with-channel req ch
    (broadcast/add broadcast/channel-store ch {})
    (on-close ch (fn [status]
                   (broadcast/end broadcast/channel-store ch)))
    (on-receive ch (fn [data])))) ; do nothing (websocket receive)

; TODO generic error handler

(defroutes app-routes
  (GET "/" [] "octoprint api proxy")
  (GET "/hydrate" [] hydrate-handler)
  (GET "/subscribe" [] subscribe-handler)
  (route/not-found "Not Found"))

(def app
  (-> (routes app-routes)
      (handler/site)))
