(ns api.broadcast
  (:require [cheshire.core :as cheshire]
            [org.httpkit.server :refer [send! websocket?]]))

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
          (send! ch {:status 200
                     :headers {"Content-Type"
                               "application/json; charset=utf-8"}
                     :body body}
                 true))))))

(defonce channel-store
  (MemoryChannelStore. (atom {})))

;; specific broadcasters

(defn broadcast-cam [store printer-id cam]
  (let [m {:type :new-cam
           :printer_id printer-id
           :timestamp (-> cam :timestamp .toString)
           :data (:data cam)}]
    (broadcast store (constantly m))))

; TODO broadcast-printers
