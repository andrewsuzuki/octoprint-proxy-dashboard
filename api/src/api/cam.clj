(ns api.cam
  (:require [clojure.core.async :refer [go-loop timeout <!]]
            [org.httpkit.client :as http]
            [clj-time.core :as t]
            [lambdaisland.uri :as uri])
  (:import (java.io ByteArrayInputStream)
           (java.net URLConnection)
           (java.util Base64)))

;; in-memory store 

(defonce cams (atom {}))

(defn byte-array-image-to-data-uri [ba]
  (when-let [mime (URLConnection/guessContentTypeFromStream (ByteArrayInputStream. ba))]
    (when-let [base64 (.encodeToString (Base64/getEncoder) ba)]
      (str "data:" mime ";base64," base64))))

(defn poll!
  "Poll a cam server asynchronously. Call provided
  callback with cam data map on valid response."
  [max-time printer-id octoprint-address callback]
  ; send the request and defer (block)
  (let [url (str (uri/join octoprint-address "/webcam/?action=snapshot"))
        {:keys [status body error]} @(http/get url {:as :byte-array, :timeout max-time})
        data-uri (byte-array-image-to-data-uri body)]
    (if error
      (throw error)
      ; light verification
      (if (and (= 200 status)
               data-uri)
        ; success!
        (let [m {:id printer-id
                 :timestamp (t/now)
                 :data data-uri}]
          (swap! cams assoc printer-id m)
          (callback printer-id m))
        (throw (Exception. (if (= 200 status)
                             "response body was not a valid image"
                             "response status was not 200")))))))

(defn should-expire?
  "Determine if a cam should expire, based on
  how much time it's been since the last update.
  Returns nil if cam doesn't exist."
  [max-seconds id]
  (when-let [cam (get @cams id)]
    (-> cam
        :timestamp
        (t/interval (t/now))
        (t/in-seconds)
        (>= max-seconds))))

;; core.async scheduler

(defmacro with-interval [ms & body]
  `(go-loop []
     ~@body
     (<! (timeout ~ms))
     (recur)))

(defn poll-start! [ms nodes callback]
  (println (str "started polling cams every " ms "ms"))
  (doseq [{:keys [id octoprint-address]} nodes]
    ; go loop for each cam server
    (with-interval ms
      (try
        (poll! ms id octoprint-address callback)
        (catch Exception e
          (println (str "Couldn't poll cam " octoprint-address ": " (.getMessage e)))
          (let [se? (should-expire? 120 id)]
            (when (true? se?)
              ; remove from cams
              (swap! cams dissoc id))
            (when se?
              ; broadcast
              (callback id nil))))))))
