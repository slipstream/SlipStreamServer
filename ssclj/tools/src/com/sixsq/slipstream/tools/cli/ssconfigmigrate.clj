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
;;
(def ^:dynamic *c-names* #{})
(def ^:dynamic *cfg-path-url* nil)
(def ^:dynamic *creds* nil)
(def ^:dynamic *modifiers* #{})

;;
;; Helper functions.
;;

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

(defn warn-con-skipped
  [cin vals]
  (println "WARNING: Skipped connector instance:" cin)
  (println "WARNING: No connector name defined for:" cin "with attrs:" vals))

(defn update-val
  [v]
  (let [nvs (for [[m r] *modifiers* :when (and (string? v) (re-find m v))]
              (s/replace v m r))]
    (if (seq nvs)
      (last nvs)
      v)))

(defn modify-vals
  [con]
  (let [res (for [[k v] con]
              [k (update-val v)])]
    (into {} res)))

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
      modify-vals
      ssconfig/store))

(defn persist-connector!
  [cin vals]
  (println "Persisting connector instance:" cin)
  (if (con-name-known? vals)
    (-> vals
        remove-attrs
        ssconfig/validate
        modify-vals
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
  (let [sc (-> *cfg-path-url* conf-xml scu/conf-xml->sc)]
    (ssconfig/init)
    (persist-config! sc)
    (persist-connectors! sc)))

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

(defn cli-parse-sets
  ([m k v]
   (cli-parse-sets m k v identity))
  ([m k v fun] (assoc m k (if-let [oldval (get m k)]
                            (merge oldval (fun v))
                            (hash-set (fun v))))))

(defn cli-parse-connectors
  [m k v]
  (cli-parse-sets m k v))

(defn ->re-match-replace
  "'m=r' -> [#'m' 'r']"
  [mr]
  (let [m-r (s/split mr #"=")]
    [(re-pattern (first m-r)) (second m-r)]))

(defn cli-parse-modifiers
  [m k v]
  (cli-parse-sets m k v ->re-match-replace))

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

