(ns api.config
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.set :refer [rename-keys]]))

(def default-config
  {:port 8080
   :cam-polling-interval 5000
   :printers []})

(def default-printer
  {:api-key nil
   :auto-connect true
   :cam-address nil})

(defonce conf (atom default-config))

(defn read-config [file]
  (let [raw (slurp file)
        c (-> raw
              (cheshire/parse-string true)
              (rename-keys {:cam_polling_interval :cam-polling-interval})
              (update :printers (fn [printers]
                                  (map
                                   (fn [printer]
                                     (let [rn (rename-keys printer {:display_name :display-name
                                                                    :octoprint_address :octoprint-address
                                                                    :api_key :api-key
                                                                    :auto_connect :auto-connect
                                                                    :cam_address :cam-address})
                                           merged (merge default-printer rn)]
                                       (when-not (and (every? #(contains? merged %) #{:display-name :octoprint-address :api-key :auto-connect :cam-address})
                                                      (every? #(not (string/blank? (% merged))) #{:display-name :octoprint-address})
                                                      (every? #(or (nil? (% merged)) (not (string/blank? (% merged)))) #{:api-key :cam-address})
                                                      (every? #(boolean? (% merged)) #{:auto-connect}))
                                         (throw (Exception. (str "bad printer config (" (:display-name merged) ")"))))
                                       (assoc merged :id (.toString (java.util.UUID/randomUUID)))))
                                   printers))))]
    (when-not (and (-> c :port int?)
                   (-> c :cam-polling-interval int?)
                   (-> c :printers seq?))
      (throw (Exception. "bad general config")))
    (swap! conf (fn [_] (merge default-config c)))))

(defn get-config [k]
  (get @conf k))
