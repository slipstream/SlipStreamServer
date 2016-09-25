(ns com.sixsq.slipstream.tools.cli.ssconfig
  (:require
    [clojure.string :as s]
    [clojure.edn :as edn]
    [clojure.tools.cli :refer [parse-opts]]

    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cfg]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as cfg-s]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as cfgt]
    [com.sixsq.slipstream.ssclj.resources.connector :as conn]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont])
  (:gen-class))

(def connector-resource conn/resource-url)
(def configuration-resource cfg/resource-url)
(def resource-types #{connector-resource
                      configuration-resource})

(def mandatory-attrs #{:id})

(def required-attrs {configuration-resource #{}
                     connector-resource     #{:cloudServiceType}})

(def ^:dynamic *templates*)

;;
;; Helper functions.
;;

(defn exit-err
  [& msg]
  (println (apply str "ERROR: " msg))
  (System/exit 1))

(defn exit-on-resp-error
  [resp]
  (if (>= (:status resp) 400)
    (let [msg (or (-> resp :body :message) (:message resp))]
      (exit-err "Failed with: " msg ".")))
  resp)

;;
;; Common functions.
;;

(defn resource-type
  "Given resource or request, returns resource type or throws."
  [c]
  (let [res (if (contains? c :id)
              (:id c)
              (-> c :body (get :id "")))]
    (if (re-find #"-template/" res)
      (first (s/split res #"-template/"))
      (first (s/split res #"/")))))

(defn connector?
  [c]
  (= (resource-type c) conn/resource-url))

(defn configuration?
  [c]
  (= (resource-type c) cfg/resource-url))

(defn check-mandatory-attrs
  [m]
  (if-not (every? #(contains? m %) mandatory-attrs)
    (exit-err "Each resource must contain following manatory attributes: " (s/join ", " mandatory-attrs))
    m))

(defn validate-resource-type
  [m]
  (if-not (contains? resource-types (resource-type m))
    (exit-err "Resource type must be one of: " (s/join ", " resource-types))
    m))

(defn resource->template-name
  [c]
  (cond
    (configuration? c) (s/replace-first (:id c) #"/" "-template/")
    (connector? c) (-> (:id c)
                       (s/split #"/")
                       first
                       (str "-template/")
                       (str (:cloudServiceType c)))
    ))

(defn get-templates
  [c]
  (cond
    (configuration? c) (:configuration *templates*)
    (connector? c) (:connector *templates*)
    :else {}))

(defn halt-no-template
  [c]
  (let [tname     (resource->template-name c)
        templates (get-templates c)]
    (when-not (contains? templates tname)
      (exit-err "No template available for " tname))
    c))

(defn check-required-attrs
  [c]
  (let [rtype (resource-type c)
        attrs (get required-attrs rtype)]
    (if-not (every? #(contains? c %) attrs)
      (exit-err "Resource '" rtype "' must contain following attributes: " (s/join ", " attrs))
      c)))

(defn validate
  [c]
  (-> c
      check-mandatory-attrs
      validate-resource-type
      check-required-attrs
      halt-no-template))

;;
;; Service configuration.
;;

(defn cfg-add-service-attr
  [m]
  (if-not (contains? m :service)
    (assoc m :service cfg-s/service)
    m))

(defn cfg-tpl-slipstream
  []
  (get (:configuration *templates*) (str cfgt/resource-url "/" cfg-s/service)))

(defn complete-config
  "Add missing keys to satisfy schema."
  [cfg]
  (->> cfg
       cfg-add-service-attr
       (merge (cfg-tpl-slipstream))))

(defn cfg-edit-or-add
  "Retruns response of edit or add operation. Tries to add if edit fails with 404."
  [config-req]
  (let [edit-resp (cfg/edit-impl config-req)]
    ;; adding may fail because template hasn't been registered yet.
    (if (= 404 (:status edit-resp))
      (do
        (println "Adding configuration for the first time.")
        (-> config-req
            (assoc :body (complete-config (:body config-req)))
            cfg/add-impl))
      edit-resp)))

(defn store-cfg
  [config]
  (-> config
      sci/cfg-as-request
      cfg-edit-or-add
      exit-on-resp-error))

;;
;; Connectors.
;;

(defn conn-name
  [conn]
  (second (s/split (:id conn) #"/")))

(defn complete-connector
  "Add missing keys to satisfy schema."
  [conn]
  (let [ctn (str cont/resource-url "/" (:cloudServiceType conn))]
    (-> (get (:connector *templates*) ctn {})
        (merge conn)
        (assoc :instanceName (conn-name conn)))))

(defn conn-edit-or-add
  "Retruns response of edit or add operation. Tries to add if edit fails with 404."
  [conn]
  (let [edit-resp (conn/edit-impl conn)]
    ;; adding may fail because template hasn't been registered yet.
    (if (= 404 (:status edit-resp))
      (do
        (println "Adding connector" (-> conn :body :id) "for the first time.")
        (-> conn
            (assoc :body (complete-connector (:body conn)))
            conn/add-impl))
      edit-resp)))

(defn store-connector
  [conn]
  (->> conn
       (sci/connector-as-request (conn-name conn))
       conn-edit-or-add
       exit-on-resp-error))

;;
;; Generic functions.
;;

(defn store
  [resource]
  (cond
    (configuration? resource) (store-cfg resource)
    (connector? resource) (store-connector resource)))

(defn run
  [files]
  (u/db-client-and-crud-impl)
  (doseq [f files]
    (-> (edn/read-string (slurp f))
        validate
        store)))

(defn list-tempates
  []
  (println "List of accessible templates.")
  (doseq [[ttn ts] *templates*]
    (println (format "- templates for '%s'" (name ttn)))
    (doseq [k (keys ts)]
      (println k))))

(defn print-template
  [tname]
  (let [ts (apply merge (vals *templates*))
        t  (get ts tname)]
    (if (seq t)
      (let [unwanted (into #{} (remove #{:id :cloudServiceType} sci/unwanted-attrs))]
        (clojure.pprint/pprint (sci/strip-unwanted-attrs t unwanted)))
      (do
        (println "WARNING: Template" tname "not found.")
        (list-tempates)))))

;;
;; Command line options processing.
;;

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (s/join \newline errors)))

(def cli-options
  [["-t" "--template TEMPLATE" "Prints out registered template by name."]
   ["-l" "--list" "List available templates."]
   ["-h" "--help"]])

(def prog-help
  "
  Ensure :id key is present in each configuration.
  Connector configuration must contain :cloudServiceType key, which corresponds to
  the connector name.

  ES_HOST and ES_PORT should be exported in the environment for the utility
  to be able to connect to Elasticsearch.")

(defn usage
  [options-summary]
  (->> [""
        "Adds or updates configuration info of configuration and connector resources in DB."
        ""
        "Usage: program-name [options] <list-of-files>"
        ""
        "Options:"
        options-summary
        ""
        "Arguments:"
        "  <list-of-files>    list of edn files with service or connector configurations."
        ""
        prog-help]
       (s/join \newline)))

(defn init
  []
  (u/initialize)
  (alter-var-root #'*templates* (constantly {:connector     @cont/templates
                                             :configuration @cfgt/templates})))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (init)
    (when (not (empty? arguments))
      (run arguments))
    (when (:list options)
      (list-tempates)
      (System/exit 0))
    (when (not (s/blank? (:template options)))
      (print-template (:template options)))))