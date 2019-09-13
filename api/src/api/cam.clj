(ns api.cam
  (:require [clj-http.client :as client]
            [clojure.core.async :refer [go timeout <!]]
            [clojure.string :as string]
            [clj-time.core :as t]
            [lambdaisland.uri :as uri]))

;; in-memory store 

(defonce cams (atom {}))

(defn poll!
  "poll multiple cam servers, where nodes is a
  collection of maps with keys :id and :cam-address."
  [max-time nodes callback]
  ; TODO make this async / make requests in parallel (using aleph instead of clj-http?)
  ; TODO make sure multiple aren't fired off to the same camera (wait for last request)
  (doseq [{:keys [id cam-address]} nodes]
    (try
      (let [url (str (uri/join cam-address "/snapshot"))
            {:keys [status body]} (client/get url {:socket-timeout max-time
                                                   :connection-timeout max-time})
            ; light validation
            data (when (and (= 200 status)
                            (string/starts-with? body "data:image/"))
                   body)
            m {:timestamp (t/now)
               :data data}]
        (when-not data
          (throw (Exception. "bad response"))) ; caught below
        (swap! cams assoc id m)
        (callback id m))
      (catch Exception e
        (println (str "Couldn't poll cam " cam-address ": " (.getMessage e)))
        ; remove
        (swap! cams dissoc id)))))

;; core.async scheduler

(defmacro with-interval [ms & body]
  `(go (loop [] (<! (timeout ~ms)) ~@body (recur))))

(defn poll-start! [ms nodes callback]
  (println (str "started polling cams every " ms "ms"))
  (with-interval ms
    (poll! ms nodes callback)))
