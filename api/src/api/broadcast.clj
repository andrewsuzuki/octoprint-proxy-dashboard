(ns api.broadcast
  (:require [cheshire.core :as cheshire]
            [org.httpkit.server :refer [send! websocket?]]
            [api.utils :refer [json-response]]))

;; channel store

(defprotocol ChannelStore
  (add [store ch attach])
  (end [store ch])
  (broadcast [store f]))

(deftype MemoryChannelStore [store-map]
  ChannelStore
  (add [_ ch attach]
    (swap! store-map (fn [old-store]
                       (assoc old-store ch attach))))
  (end [_ ch]
    (swap! store-map (fn [old-store]
                       (dissoc old-store ch))))
  (broadcast [store f]
    (doseq [[ch attach] @store-map]
      (let [body (cheshire/generate-string (f ch attach))]
        (if (websocket? ch)
          (send! ch body false)
          (send! ch (json-response body) true))))))

(defonce channel-store
  (MemoryChannelStore. (atom {})))

;; specific broadcasters

(defn stringify-timestamp-m [m]
  (update m :timestamp str))

(defn broadcast-cam! [store printer-id cam]
  (if cam
    (let [cam-final (stringify-timestamp-m cam)
          m {:type :new-cam
             :printer-id printer-id
             :timestamp (:timestamp cam-final)
             :data cam-final}]
      (broadcast store (constantly m)))
    (let [m {:type :remove-cam
             :printer-id printer-id}]
      (broadcast store (constantly m)))))

(defn broadcast-printer! [store printer]
  (let [printer-final (-> printer
                          stringify-timestamp-m
                          (dissoc :username :password))
        m {:type :new-printer
           :printer-id (:id printer-final)
           :timestamp (:timestamp printer-final)
           :data printer-final}]
    (broadcast store (constantly m))))
