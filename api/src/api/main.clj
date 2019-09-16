(ns api.main
  (:use [org.httpkit.server :only [run-server]])
  (:require [ring.middleware.reload :as reload]
            [clojure.java.io :refer [resource]]
            [clojure.string :as string]
            [clojure.tools.cli :refer [parse-opts]]
            [api.config :refer [read-config! get-config]]
            [api.handler :as handler]
            [api.cam :as cam]
            [api.octoprint :as octoprint]
            [api.broadcast :as broadcast])
  (:gen-class))

(def cli-options
  [["-p" "--port PORT" "Port number for service"
    :id :port
    :default 8080
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-o" "--origins ALLOWED_ORIGINS" "Allowed origins (value of Access-Control-Allow-Origin response header)"
    :id :allowed-origins
    :default "*"
    :parse-fn str]
   ["-c" "--config CONFIG_FILE" "Path to config file"
    :id :config
    :missing "Config file is required"
    :validate [#(not (string/blank? %)) "Must not be blank"]]
   ["-e" "--env ENV" "Environment (dev or prod)"
    :id :env
    :default "prod" ; => :prod
    :missing "Env is required"
    :parse-fn keyword
    :validate [#(contains? #{:dev :prod} %) "Must be dev or prod"]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["octoprint proxy"
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
  (let [{:keys [exit-message ok? env config port allowed-origins]} (validate-args args)
        dev? (= env :dev)]
    (when exit-message
      (exit! (if ok? 0 1) exit-message))
    (when dev?
      (println "--in development mode--"))
    ; read config from config file
    (try
      (read-config! config)
      (catch Throwable e
        (exit! 1 (str "Error reading config: " (.getMessage e)))))
    ; start server
    (let [reloaded-handler (if dev?
                             (reload/wrap-reload #'handler/app) ;; only reload when dev
                             handler/app)
          cors-handler (handler/wrap-cors reloaded-handler allowed-origins)]
      (run-server cors-handler {:port port})
      (println (str "started api on port " port)))
    ; start cam polling
    (cam/poll-start! (get-config :cam-polling-interval)
                     ;; printers
                     (->> (get-config :printers)
                          (filter :cam-address)
                          (map (fn [printer]
                                 (select-keys printer [:id :cam-address]))))
                     ;; broadcast callback
                     (fn [id cam]
                       (broadcast/broadcast-cam! broadcast/channel-store id cam)))
    ; connect to octoprints
    (let [printer-configs (->> (get-config :printers)
                               (map (fn [printer]
                                      (select-keys printer [:id :display-name :octoprint-address]))))
          callback (fn [printer]
                     (broadcast/broadcast-printer! broadcast/channel-store printer))]
      (doseq [{:keys [id display-name octoprint-address]} printer-configs]
        (octoprint/connect! (get-config :octoprint-reconnect-interval)
                            id
                            display-name
                            octoprint-address
                            callback)))))
