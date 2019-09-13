(ns cam.config)

(defonce config-map
  (atom {:device nil}))

(defn set-config [k v]
  (swap! config-map assoc k v))

(defn get-config [k]
  (get @config-map k))
