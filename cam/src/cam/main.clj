(ns cam.main
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.reload :as reload]
            [clojure.java.io :refer [resource]]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [cam.handler :refer [app]])
  (:gen-class))

(def cli-options
  [["-d" "--device DEVICE" "Camera device"
    :id :device
    :missing "Camera device is required"
    :validate [#(not (string/blank? %)) "Must not be blank"]]
   ["-p" "--port PORT" "Port number for service"
    :id :port
    :default 8020
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-e" "--env ENV" "Environment (dev or prod)"
    :id :env
    :default "prod" ; => :prod
    :parse-fn keyword
    :validate [#(contains? #{:dev :prod} %) "Must be dev or prod"]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["cam snapshot service"
        ""
        "Options:"
        options-summary]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      :else ; else => continue with parsed options
      options)))

(defn exit! [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [exit-message ok? env port]} (validate-args args)
        dev? (= env :dev)]
    (when exit-message
      (exit! (if ok? 0 1) exit-message))
    (when dev?
      (println "--in development mode--"))
    ; start server
    (let [reloaded-handler (if dev?
                             (reload/wrap-reload #'app) ;; only reload when dev
                             app)]
      (run-server reloaded-handler {:port port})
      (println (str "started cam service on port " port)))))
