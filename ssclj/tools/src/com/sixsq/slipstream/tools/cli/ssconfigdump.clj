(ns com.sixsq.slipstream.tools.cli.ssconfigdump
  (:require
    [clojure.string :as s]
    [clojure.tools.cli :refer [parse-opts]]

    [com.sixsq.slipstream.tools.cli.utils :refer :all]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu])
  (:gen-class))

;;
;; Dynamic vars.
(def ^:dynamic *c-names* #{})
(def ^:dynamic *cfg-path-url* nil)
(def ^:dynamic *creds* nil)
(def ^:dynamic *modifiers* #{})
(def ^:dynamic *skip-no-connector-name* false)

(defn dump-configuration!
  [sc]
  (println "Saving configuration:" "slipstream")
  (-> sc
      sci/sc->cfg
      (modify-vals *modifiers*)
      (scu/spit-pprint (format "configuration-%s.edn" "slipstream"))))

(defn dump-connector!
  [cn vals desc]
  (println "Saving connector:" cn)
  (scu/spit-pprint vals (format "connector-%s.edn" cn))
  (scu/spit-pprint desc (format "connector-%s-desc.edn" cn)))

(defn blank-category
  [desc]
  (into {}
        (for [[k m] desc]
          [k (assoc m :category "")])))

(defn dump-connectors!
  [sc]
  (doseq [[cnkey [vals desc]] (sci/sc->connectors sc *c-names*)]
    (if (and (seq desc) (seq (dissoc vals :id :cloudServiceType)))
      (if (and (not (:cloudServiceType vals)) *skip-no-connector-name*)
        (println "WARNING: :cloudServiceType is not defined. Not dumping" (:id vals))
        (dump-connector! (name cnkey) (modify-vals vals *modifiers*) (blank-category desc)))
      (println "WARNING: No data obtained for connector:" (name cnkey)))))

(defn run
  []
  (let [sc (cfg-path-url->sc *cfg-path-url* *creds*)]
    (dump-configuration! sc)
    (dump-connectors! sc)))

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
   [nil "--skip-no-connector-name" "Don't dump invalid configurations."]
   ["-h" "--help"]])

(def prog-help
  "
  Given SlipStream URL or path to file with configuration XML, extracts
  and stores global service configuration and per connector parameters,
  with their description, into connfiguration-slipstream.edn,
  connector-<instance-name>.edn and connector-<instance-name>-desc.edn
  respectively.")

(defn usage
  [options-summary]
  (->> [""
        "Extracts and stores service configuration, connector parameters
        and their description."
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
          credentials (:credentials options)]
      (if (empty? configxml)
        (exit 1 (error-msg "-x parameter must be provided."))
        (alter-var-root #'*cfg-path-url* (fn [_] configxml)))
      (if (and (s/starts-with? configxml "https") (empty? credentials))
        (exit 1 (error-msg "-s must be provided when -x is URL."))
        (alter-var-root #'*creds* (fn [_] credentials)))
      (alter-var-root #'*c-names* (fn [_] (:connectors options)))
      (alter-var-root #'*modifiers* (fn [_] (:modifiers options)))
      (alter-var-root #'*skip-no-connector-name* (fn [_] (:skip-no-connector-name options)))))
  (run)
  (System/exit 0))

