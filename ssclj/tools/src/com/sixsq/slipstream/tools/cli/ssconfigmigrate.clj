(ns com.sixsq.slipstream.tools.cli.ssconfigmigrate
  (:require
    [clojure.string :as s]
    [clojure.tools.cli :refer [parse-opts]]

    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]

    [com.sixsq.slipstream.tools.cli.ssconfig :as ssconfig]
    [com.sixsq.slipstream.tools.cli.utils :refer :all])
  (:gen-class))

;;
;; Dynamic vars.
;;
(def ^:dynamic *c-names* #{})
(def ^:dynamic *cfg-path-url* nil)
(def ^:dynamic *creds* nil)
(def ^:dynamic *modifiers* #{})

;;
;; Helper functions.
;;


(defn warn-con-skipped
  [cin vals]
  (println "WARNING: Skipped connector instance:" cin)
  (println "WARNING: No connector name defined for:" cin "with attrs:" vals))

;;
;; Persistence.
;;

(def con-attrs-to-remove
  {"ec2"      [:securityGroup]
   "nuvlabox" [:orchestratorInstanceType :pdiskEndpoint]})

(defn remove-attrs
  "Cleanup of old attributes during migration."
  [con]
  (if-let [attrs (get con-attrs-to-remove (:cloudServiceType con))]
    (apply dissoc con attrs)
    con))

(defn con-name-known?
  [con]
  (not (s/blank? (:cloudServiceType con))))

(defn persist-config!
  [sc]
  (println "Peristing global configuration.")
  (-> sc
      sci/sc->cfg
      ssconfig/validate
      (modify-vals *modifiers*)
      ssconfig/store))

(defn persist-connector!
  [cin vals]
  (println "Persisting connector instance:" cin)
  (if (con-name-known? vals)
    (-> vals
        remove-attrs
        ssconfig/validate
        (modify-vals *modifiers*)
        ssconfig/store)
    (warn-con-skipped cin vals)))

(defn persist-connectors!
  [sc]
  (doseq [[cinkey vals] (sci/sc->connectors-vals-only sc *c-names*)]
    (if (seq (dissoc vals :id :cloudServiceType))
      (persist-connector! (name cinkey) vals)
      (println "WARNING: No data obtained for connector instance:" (name cinkey)))))

(defn run
  []
  (let [sc (cfg-path-url->sc *cfg-path-url* *creds*)]
    (ssconfig/init)
    (persist-config! sc)
    (persist-connectors! sc)))

;;
;; Command line options processing.
;;

(def cli-options
  [["-c" "--connector CONNECTOR" "Connector instance names (category). If not provided all connectors will be stored."
    :id :connectors
    :default #{}
    :assoc-fn cli-parse-connectors]
   ["-x" "--configxml CONFIGXML" "Path to file or URL starting with https (requries -s parameter). Mandatory."]
   ["-s" "--credentials CREDENTIALS" "Credentials as user:pass for -x when URL is provided."]
   ["-m" "--modify old=new" "Modify in all values. '=' is a separator. Usefull for updating hostname/ip of SlipStream."
    :id :modifiers
    :default #{}
    :assoc-fn cli-parse-modifiers]
   ["-h" "--help"]])

(def prog-help
  "
  Given SlipStream URL or path to file with configuration XML, extracts
  and stores the global service and per connector configuration
  parameters into DB backend identified by ES_HOST and ES_PORT env vars.")

(defn usage
  [options-summary]
  (->> [""
        "Migrates SlipStream service configuration from a running instance or XML file"
        "to DB backend identified by ES_HOST and ES_PORT env vars."
        ""
        "Usage: -x <path or URL> [-s <user:pass>] [-c <connector name>]"
        ""
        "Options:"
        options-summary
        prog-help]
       (s/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (let [configxml   (:configxml options)
          credentials (:credentials options)
          connectors  (:connectors options)
          modifiers   (:modifiers options)]
      (if (empty? configxml)
        (exit 1 (error-msg "-x parameter must be provided."))
        (alter-var-root #'*cfg-path-url* (fn [_] configxml)))
      (if (and (s/starts-with? configxml "https") (empty? credentials))
        (exit 1 (error-msg "-s must be provided when -x is URL."))
        (alter-var-root #'*creds* (fn [_] credentials)))
      (alter-var-root #'*c-names* (fn [_] connectors))
      (alter-var-root #'*modifiers* (fn [_] modifiers))))
  (run)
  (System/exit 0))

