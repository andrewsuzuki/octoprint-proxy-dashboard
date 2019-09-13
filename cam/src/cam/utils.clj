(ns cam.utils
  (:require [ring.util.response :refer [response header]]
            [cheshire.core :as cheshire]))

(defn json-response
  [m]
  (-> m
      cheshire/generate-string
      response
      (header "Content-Type" "application/json; charset=utf-8")))
