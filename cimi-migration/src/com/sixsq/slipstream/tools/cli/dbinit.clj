(ns com.sixsq.slipstream.tools.cli.dbinit
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [com.sixsq.slipstream.db.loader :as db-loader]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.tools.cli.utils :as u]
    [taoensso.timbre :as log])
  (:gen-class))


(def default-db-binding-ns "com.sixsq.slipstream.db.es.loader")


(def valid-logging-levels #{:trace :debug :info :warn :error :fatal :report})


(defn init-db-client
  [binding-ns]
  (db-loader/load-and-set-persistent-db-binding binding-ns))


(def cli-options
  [["-b" "--binding BINDING_NS" "Database binding namespace."
    :id :binding-ns
    :default default-db-binding-ns]
   ["-h" "--help"]
   ["-l" "--logging LEVEL" "Logging level: trace, debug, info, warn, error, fatal, or report."
    :id :level
    :default :info
    :parse-fn #(-> % str/lower-case keyword)]])


(def prog-help
  "
  Initialize a persistent database using the specified database binding.
  Iterates over all defined CIMI resources and runs each resource's
  initialization function.

  By default, this will use an Elasticsearch binding. You must define ES_HOST
  and ES_PORT to identify the server to initialize. ES_HOST will default to
  'localhost' and ES_PORT will default to 9200 or 9300 depending on the
  binding.")


(defn usage
  [options-summary]
  (str/join \newline
            [""
             "Initializes a persistent database for SlipStream resources."
             ""
             "Usage: [options]"
             ""
             "Options:"
             options-summary
             ""
             prog-help]))


(defn -main
  [& args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]

    (log/set-level! (or (valid-logging-levels (:level options)) :info))

    (log/debug "parsed command line options:\n" (with-out-str (pprint options)))

    ;; help and command line option errors
    (cond
      (:help options) (u/success (usage summary))
      errors (u/failure (u/error-msg errors)))

    (log/debug "initializing the database binding: " (:binding-ns options))

    ;; initialize the database binding
    (if-let [binding-ns (:binding-ns options)]
      (init-db-client binding-ns)
      (u/failure "database binding namespace must be specified (-b, --binding)"))

    (log/debug "initializing the database resources")

    ;; actually initialize the database
    (dyn/initialize)

    (u/success)))
