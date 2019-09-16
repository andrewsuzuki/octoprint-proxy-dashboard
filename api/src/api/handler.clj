(ns api.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [org.httpkit.server :refer [with-channel on-close send! on-receive]]
            [api.broadcast :as broadcast]
            [api.cam :as cam]
            [api.octoprint :as octoprint]
            [api.utils :refer [json-response plain-response with-allow-origin]]))

(defn hydrate-handler
  "hydrate new client with current state"
  [req]
  (letfn [(vals-and-stringify-timestamps [els]
            (map broadcast/stringify-timestamp-m
                 (or (vals els) [])))]
    (let [m {:cams (into {} (for [[k v] @cam/cams] [k (broadcast/stringify-timestamp-m v)]))
             :printers (vals-and-stringify-timestamps @octoprint/printers)}]
      (json-response m))))

(defn subscribe-handler
  "stream (websocket or http long-poll) updates to client"
  [req]
  (with-channel req ch
    (broadcast/add broadcast/channel-store ch {})
    (on-close ch (fn [status]
                   (broadcast/end broadcast/channel-store ch)))
    (on-receive ch (fn [data])))) ; do nothing (websocket receive)

(defn wrap-remaining-exceptions
  "Unhandled exception response middleware (catch-all)"
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        ; catch-all
        (plain-response "An unknown error occurred" 500)))))

(defroutes app-routes
  (GET "/" [] (plain-response "octoprint proxy service" 200))
  (GET "/hydrate" [] hydrate-handler)
  (GET "/subscribe" [] subscribe-handler)
  (route/not-found (plain-response "not found" 404)))

(def app
  (-> (routes app-routes)
      (handler/site)
      (wrap-remaining-exceptions)))

(defn wrap-cors
  "Add Access-Control-Allow-Origin header
  (applied in main.clj)"
  [handler origin-str]
  (fn [req]
    (with-allow-origin (handler req) origin-str)))
