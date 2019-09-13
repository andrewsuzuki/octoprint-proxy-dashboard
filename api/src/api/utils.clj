(ns api.utils
  (:require [ring.util.response :refer [response header status]]
            [cheshire.core :as cheshire]))

(defn json-response
  [m]
  (-> m
      cheshire/generate-string
      response
      (header "Content-Type" "application/json; charset=utf-8")))

(defn plain-response
  [t s]
  (-> t
      response
      (status s)
      (header "Content-Type" "text/plain; charset=utf-8")))
