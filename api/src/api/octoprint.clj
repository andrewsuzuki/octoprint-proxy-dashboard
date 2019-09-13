(ns api.octoprint
  "facilitate subscribing to octoprints and forwarding their updates"
  (:require [clj-time.core :as t]
            [aleph.http :as http]
            [manifold.stream :as stream]
            [cheshire.core :as cheshire]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as string]
            [clj-time.coerce :as c]
            [lambdaisland.uri :as uri]))

;; Printer state shape:
;; {:id string (uuid)
;;  :display-name string
;;  :status #{:connected :disconnected :unreachable (connection attempted but failed) :incompatible (incompatible version)}
;;  :timestamp JodaTime (last received message)
;;  :connection {:version string}
;;  :general {:state {:text string
;;                           ; ALL BOOLS:
;;                    :flags {:operational :paused :printing :pausing
;;                            :cancelling :sd-ready :error :ready :closed-or-error}}
;;            :job {:file {:name string}
;;                  :times {:estimated int
;;                          :last int}
;;                  :filament {:length int
;;                             :volume float}}
;;            :progress {:percent float
;;                       :seconds-spent int
;;                       :seconds-left int}
;;            :current-z float
;;            :offsets TODO 
;;            :temps [{:name string
;;                     :actual float|null
;;                     :target float|null
;;                     :offset float|null}
;;                   ...] 
;;            }
;;  :slicer {:source-path string
;;           :progress float
;;           :last-slicing-progress int (our unix epoch timestamp, see logic in wipe-slicer-maybe)
;;           }}

;; State

(defonce printers (atom {}))
(defonce connections (atom {}))

;; Octoprint message types and payloads http://docs.octoprint.org/en/master/api/push.html
;; We listen to `connected`, `current`/`history`, and `slicingProgress`.

(defn assoc-current-timestamp [printer]
  (assoc printer :timestamp (t/now)))

(defn init-printer-state [printer-id display-name status]
  (assoc-current-timestamp
   {:id printer-id
    :display-name display-name
    :status status
    :connection nil :general nil :slicer nil}))

(defn compatible-version?
  "Is the proxy compatible with an octoprint's version?
  a bit primitive, and should be checked once 1.4 is released.
  (NOTE currently developing against octoprint 1.3.11).
  Considering >= 1.2.0 to be compatible for now (though
  there are no promises for future versions to be compatible)."
  [version]
  (let [[major minor patch] (map #(Integer/parseInt %) (string/split version #"\."))]
    (cond
      (> 1 major) false ; 0.x.x, no
      (= 1 major) (<= 2 minor) ; 1.x.x, make sure it's equal to or above 1.2.0
      (< 1 major) true))) ; 2+.x.x, yes, but don't know

(defn select-keys-default
  "select-keys, but if keys don't exist,
  include in m with default value"
  [m default keys]
  (reduce #(assoc %1 %2 (get m %2 default)) {} keys))

(defn derive-temps
  "parse temps from octoprint"
  [points]
  ; get latest data point (the last in the list, if not empty)
  (when-let [latest (last points)]
    (reduce-kv (fn [acc k v]
                 (if (or (= k :bed)
                         (-> k name (string/starts-with? "tool")))
                   (conj acc
                         (-> v
                             (select-keys-default nil [:actual :target :offset])
                             (assoc :name k)))
                   acc))
               (list)
               latest)))

(defn derive-general
  "helper for generating :general state from
  :current and :history messages (in transform-payload)"
  [p]
  {:state {:text (-> p :state :text)
           :flags (-> p
                      :state
                      :flags
                      (rename-keys {:closedOrError :closed-or-error
                                    :sdReady :sd-ready})
                      (select-keys-default false [:operational :paused :printing :pausing
                                                  :cancelling :sd-ready :error :ready :closed-or-error]))}
   :job {:file {:name (-> p :job :file :name)}
         :times {:estimated (-> p :job :estimatedPrintTime)
                 :last (-> p :job :lastPrintTime)}
         :filament (-> p :job :filament (select-keys-default nil [:length :volume]))}
   :progress {:percent (-> p :progress :completion)
              :seconds-spent (-> p :progress :printTime)
              :seconds-left (-> p :progress :printTimeLeft)}
   :current-z (-> p :currentZ)
   :offsets nil ; TODO offsets
   :temps (-> p :temps derive-temps)})

; TODO test this thoroughly at makehaven
(defn wipe-slicer-maybe
  [state type payload]
  (if (case type
        ; if slicingProgress, wipe it if it's 100
        :slicingProgress (->> state :slicer :progress int (= 100))
        ; if event, check if this is an event that implies no more slicingProgress messages are coming
        :event (->> payload
                    :type ; (the event type, not the message type)
                    (contains? #{"SlicingDone" "SlicingCancelled" "SlicingFailed"}))
        ; otherwise, check the state's :last-slicing-progress
        ; and check if it was more than 120 seconds ago
        (when-let [lsp (get-in state [:slicer :last-slicing-progress])]
          (< 120 (- (c/to-epoch (t/now)) lsp))))
    ; yes, wipe it
    (dissoc state :slicer)
    ; no, do nothing
    state))

(defn transform-payload
  "Transform an Octoprint Push API update payload
  into watered-down proxy printer state,
  given current printer state."
  [state type payload]
  ; check version for compatibility
  (cond
    ; new :connected message, but incompatible. re-init state, marked :incompatible
    (and (= type :connected) (-> payload :version compatible-version? not))
    (init-printer-state (:id state) (:display-name state) :incompatible)
    ; new message, but existing state is marked incompatible. do nothing
    (-> state :status (= :incompatible))
    state
    ; new message, ok
    :else
    (-> (case type
          :connected (let [{:keys [version]} payload
                           connection {:version version}]
                       (assoc state :connection connection))
          (:current :history) (assoc state :general (derive-general payload))
          :slicingProgress (let [{:keys [source_path progress]} payload
                                 slicer {:source-path source_path
                                         :progress progress
                                         ; set unix timestamp for this slicing progress
                                         ; and use it for later comparison (see wipe-slicer-maybe)
                                         :last-slicing-progress (c/to-epoch (t/now))}]
                             (assoc state :slicer slicer))
          state)
        (assoc-current-timestamp)
        (assoc :status :connected) ; ensure status is marked :connected
        (wipe-slicer-maybe type payload))))

;; Communication

(defn on-closed [printer-id callback]
  ; remove connection
  (swap! connections dissoc printer-id)
  ; clean (re-initialize) printer state
  (swap! printers update printer-id
         (fn [state]
           (init-printer-state (:id state) (:display-name state) :disconnected)))
  ; callback
  (callback (get @printers printer-id)))

(defn on-message [printer-id body callback]
  (let [current (get @printers printer-id)
        ; transform message body given current state. this uses
        ; reduce to allow for multiple keys, but the message body map
        ; should actually just have a single entry (<type> => payload).
        transformed (reduce-kv transform-payload current body)]
    ; update
    (swap! printers assoc printer-id transformed)
    ; callback
    (callback (get @printers printer-id))))

(defn derive-uri
  "Derive Octoprint Push API uri string from the configured base server address.
  Uses SockJS's raw websocket feature, since there isn't a clojure/java SockJS client:
  https://github.com/sockjs/sockjs-client#connecting-to-sockjs-without-the-client"
  [address]
  (let [parsed (uri/uri address)]
    (-> parsed
        (assoc :scheme (case (:scheme parsed) ; change scheme to ws[s]
                         "http" "ws"
                         "https" "wss"
                         (throw (Exception. "invalid octoprint address"))))
        (uri/join "/sockjs/websocket") ; append sockjs/websocket to path
        (str))))

(defn connect!
  "connect to octoprint push api websocket using aleph,
  setting it up to consume new messages."
  [printer-id display-name address callback]
  ; initialize printer state
  (swap! printers assoc printer-id (init-printer-state printer-id display-name :disconnected))
  ; attempt connection to websocket
  ; TODO X-Api-Key header needed?
  (when-let [conn (try
                    @(http/websocket-client (derive-uri address)
                                            {:max-frame-payload 2621440}) ; increase to 2.5MB; 65536 wasn't enough
                    (catch Exception e
                      ; couldn't connect; mark as :unreachable
                      (swap! printers assoc printer-id (init-printer-state printer-id display-name :unreachable))
                      (println (str "Couldn't reach Octoprint " address))
                      ; return nil (don't execute below body)
                      nil))]
    (stream/on-closed conn (fn [] (on-closed printer-id callback)))
    (stream/consume (fn [message]
                      (on-message printer-id
                                  (cheshire/parse-string message true)
                                  callback))
                    conn)
    (swap! connections assoc printer-id conn)))

;; TODO auto-connect (proxy to octoprint)

;; TODO auto-connect (octoprint to printer)