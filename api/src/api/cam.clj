(ns api.cam
  (:require [clojure.core.async :refer [go-loop timeout <!]]
            [clojure.string :as string]
            [org.httpkit.client :as http]
            [clj-time.core :as t]
            [lambdaisland.uri :as uri]))

;; in-memory store 

(defonce cams (atom {}))

(defn poll!
  "Poll a cam server asynchronously. Call provided
  callback with cam data map on valid response."
  [max-time printer-id cam-address callback]
  ; send the request and defer (block)
  (let [url (str (uri/join cam-address "/snapshot"))
        {:keys [status body error]} @(http/get url {:as :text :timeout max-time})]
    (if error
      (throw error)
      ; light verification
      (if (and (= 200 status)
               (string/starts-with? body "data:image/"))
        ; success!
        (let [m {:id printer-id
                 :timestamp (t/now)
                 :data body}]
          (swap! cams assoc printer-id m)
          (callback printer-id m))
        (throw (Exception. (if (= 200 status)
                             "response body was not data uri image"
                             "response status was not 200")))))))

;; core.async scheduler

(defmacro with-interval [ms & body]
  `(go-loop []
     ~@body
     (<! (timeout ~ms))
     (recur)))

(defn poll-start! [ms nodes callback]
  (println (str "started polling cams every " ms "ms"))
  (doseq [{:keys [id cam-address]} nodes]
    ; go loop for each cam server
    (with-interval ms
      (try
        (poll! ms id cam-address callback)
        (catch Exception e
          (println (str "Couldn't poll cam " cam-address ": " (.getMessage e)))
          ; remove
          (swap! cams dissoc id))))))
