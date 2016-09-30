(ns com.sixsq.slipstream.tools.cli.ssconfigmigrate
  (:require
    [clojure.string :as s]
    [clj-http.client :as http]
    [clojure.tools.cli :refer [parse-opts]]

    [me.raynes.fs :as fs]

    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]

    [com.sixsq.slipstream.tools.cli.ssconfig :as ssconfig]
    [com.sixsq.slipstream.db.serializers.utils :as u])
  (:gen-class))

;;
;; Dynamic vars.
(def ^:dynamic *c-names* #{})
(def ^:dynamic *cfg-path-url* nil)
(def ^:dynamic *creds* nil)

(defn fwrite
  [o fpath]
  (let [f (fs/expand-home fpath)]
    (with-open [^java.io.Writer w (apply clojure.java.io/writer f {})]
      (clojure.pprint/pprint o w))))

(defn ->config-resource
  [url]
  (str url "/configuration"))

(defn conf-xml
  [path-url]
  (if (s/starts-with? path-url "https")
    (-> path-url
        ->config-resource
        (http/get {:follow-redirects false
                   :accept           :xml
                   :basic-auth       *creds*})
        :body)
    (slurp path-url)))

(defn persist-connector!
  [cn vals]
  (println "Persisting connector:" cn)
  (-> vals
      ssconfig/validate
      ssconfig/store))

(defn run
  []
  (let [sc (-> *cfg-path-url* conf-xml scu/conf-xml->sc)]
    (ssconfig/init)
    (doseq [[cnkey vals] (sci/sc->connectors-vals-only sc *c-names*)]
      (if (seq (dissoc vals :id :cloudServiceType))
        (persist-connector! (name cnkey) vals)
        (println "WARNING: No data obtained for connector:" (name cnkey))))))

;;
;; Command line options processing.
;;

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn error-msg
  [& errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(defn cli-parse-connectors
  [m k v] (assoc m k (if-let [oldval (get m k)]
                       (merge oldval v)
                       (hash-set v))))

(def cli-options
  [["-c" "--connector CONNECTOR" "Connector instance names (category). If not provided all connectors will be stored."
    :id :connectors
    :default #{}
    :assoc-fn cli-parse-connectors]
   ["-x" "--configxml CONFIGXML" "Path to file or URL starting with https (requries -s parameter). Mandatory."]
   ["-s" "--credentials CREDENTIALS" "Credentials as user:pass for -x when URL is provided."]
   ["-h" "--help"]])

(def prog-help
  "
  Given SlipStream URL or path to file with configuration XML, extracts
  and stores per connector parameters and their description into
  connector-<instance-name>.edn and connector-<instance-name>-desc.edn
  respectively.")

(defn usage
  [options-summary]
  (->> [""
        "Extracts and stores connector parameters and their description."
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
          connectors  (:connectors options)]
      (if (empty? configxml)
        (exit 1 (error-msg "-x parameter must be provided."))
        (alter-var-root #'*cfg-path-url* (fn [_] configxml)))
      (if (and (s/starts-with? configxml "https") (empty? credentials))
        (exit 1 (error-msg "-s must be provided when -x is URL."))
        (alter-var-root #'*creds* (fn [_] credentials)))
      (alter-var-root #'*c-names* (fn [_] connectors))))
  (run)
  (System/exit 0))

