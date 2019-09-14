(ns api.cam
  (:require [clojure.core.async :refer [go-loop timeout <!]]
            [clojure.string :as string]
            [org.httpkit.client :as http]
            [clj-time.core :as t]
            [lambdaisland.uri :as uri]))

;; in-memory store 

(defonce cams (atom {}))

(defn poll!
  "poll multiple cam servers asynchronously,
  where nodes is a collection of maps with
  keys :id and :cam-address. Call provided
  callback with cam data map on valid response."
  [max-time nodes callback]
  (letfn [(on-response [{:keys [status body error opts]}]
            (let [{:keys [id cam-address]} opts] ; destruct custom state from opts
              (if error
                (do
                  (println (str "Couldn't poll cam " cam-address ": " (.getMessage error)))
                  ; remove
                  (swap! cams dissoc id))
                ; light verification
                (when (and (= 200 status)
                           (string/starts-with? body "data:image/"))
                  ; success!
                  (let [m {:id id
                           :timestamp (t/now)
                           :data body}]
                    (swap! cams assoc id m)
                    (callback id m))))))]
    ; send the requests to each cam asynchronously
    (doseq [{:keys [id cam-address]} nodes]
      (let [url (str (uri/join cam-address "/snapshot"))]
        (http/get url
                  {:as :text
                   :timeout max-time
                   ; custom state:
                   :id id
                   :cam-address cam-address}
                  on-response)))))

;; core.async scheduler

(defmacro with-interval [ms & body]
  `(go-loop []
     ~@body
     (<! (timeout ~ms))
     (recur)))

(defn poll-start! [ms nodes callback]
  (println (str "started polling cams every " ms "ms"))
  (with-interval ms
    (poll! ms nodes callback)))
