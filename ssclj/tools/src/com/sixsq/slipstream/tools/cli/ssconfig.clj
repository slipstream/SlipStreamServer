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
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont]
    [com.sixsq.slipstream.ssclj.resources.connector :as con])
  (:gen-class))

(def connector-resource conn/resource-url)
(def configuration-resource cfg/resource-url)
(def resource-types #{connector-resource
                      configuration-resource})

(def mandatory-attrs #{:id})

(def required-attrs {configuration-resource #{}
                     connector-resource     #{:cloudServiceType}})

(def ^:dynamic *templates*)
(def ^:dynamic *resources*)

;;
;; Helper functions.
;;

(defn exit-err
  [& msg]
  (println (apply str "ERROR: " msg))
  (System/exit 1))

(defn resp-success?
  [resp]
  (< (:status resp) 400))

(defn resp-error?
  [resp]
  (not (resp-success? resp)))

(defn exit-on-resp-error
  [resp]
  (if (resp-error? resp)
    (let [msg (or (-> resp :body :message) (:message resp))]
      (exit-err "Failed with: " msg ".")))
  resp)

(defn init-db-client
  []
  (u/db-client-and-crud-impl))

(defn init-namespaces
  []
  (u/initialize)
  (alter-var-root #'*templates* (constantly {:connector     @cont/templates
                                             :configuration @cfgt/templates})))

(defn init
  []
  (init-db-client)
  (init-namespaces))

;;
;; Common functions.
;;

(defn resource-type-from-str
  [rstr]
  (if (re-find #"-template/" rstr)
    (first (s/split rstr #"-template/"))
    (first (s/split rstr #"/"))))

(defn resource-type
  "Given string, resource or request, returns resource type or throws."
  [c]
  (let [res (if (string? c)
              c
              (if (contains? c :id)
                (:id c)
                (-> c :body (get :id ""))))]
    (resource-type-from-str res)))

(defn connector?
  "c - str or map."
  [c]
  (= (resource-type c) conn/resource-url))

(defn configuration?
  "c - str or map."
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

(defn conn-inst-name
  [conn]
  (second (s/split (:id conn) #"/")))

(defn complete-connector
  "Add missing keys to satisfy schema."
  [conn]
  (let [ctn (str cont/resource-url "/" (:cloudServiceType conn))]
    (-> (get (:connector *templates*) ctn {})
        (merge conn)
        (assoc :instanceName (conn-inst-name conn)))))

(defn get-ccc
  []
  (:cloudConnectorClass (sci/load-cfg)))

(defn merge-ccc-and-new-conn
  [ccc new-conn]
  (s/replace (s/join "," [ccc new-conn]) #"(^[ \t]*,*|,*[ \t]*$)" ""))

(defn update-ccc
  [ccc cin cn]
  (let [new-conn (s/join ":" [cin cn])]
    (println "Updating :cloudConnectorClass parameter with -" new-conn)
    (store-cfg {:id                  (str cfg/resource-url "/" cfg-s/service)
                :cloudConnectorClass (merge-ccc-and-new-conn ccc new-conn)})))

(defn add-conn-to-ccc
  "Adds connector to :cloudConnectorClass attribute."
  [cin cn]
  (let [ccc     (get-ccc)
        cin->cn (sci/connector-names-map ccc)]
    (if-not (and (contains? cin->cn cin) (= (get cin->cn cin) cn))
      (update-ccc ccc cin cn)
      {:status  200
       :message "Connector was already in :cloudConnectorClass."})))

(defn conn-add
  "Connector `conn` as request. The resource is in :body."
  [conn]
  (let [cin      (conn-inst-name (:body conn))
        cn       (-> conn :body :cloudServiceType)
        add-resp (-> conn
                     (assoc :body (complete-connector (:body conn)))
                     conn/add-impl)]
    (if (resp-success? add-resp)
      (add-conn-to-ccc cin cn)
      add-resp)))

(defn conn-edit-or-add
  "Retruns response of edit or add operation. Tries to add if edit fails with 404."
  [conn]
  (let [edit-resp (conn/edit-impl conn)]
    ;; adding may fail because template hasn't been registered yet.
    (if (= 404 (:status edit-resp))
      (do
        (println "Adding connector" (-> conn :body :id) "for the first time.")
        (conn-add conn))
      edit-resp)))

(defn store-connector
  [conn]
  (->> conn
       (sci/connector-as-request (conn-inst-name conn))
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
  (init-db-client)
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

(defn resource-uuid
  [rname]
  (-> rname (s/split #"/") second))

(defn get-configuration-resource
  [rname]
  (sci/get-configuration (resource-uuid rname)))

(defn get-connector-resource
  [rname]
  (sci/get-connector (resource-uuid rname)))

(defn get-resource
  [rname]
  (cond
    (configuration? rname) (get-configuration-resource rname)
    (connector? rname) (get-connector-resource rname)
    :else (println "WARNING: Don't know how to get resource:" rname)))

(defn print-resource
  [r]
  (let [unwanted (into #{} (remove #{:id :cloudServiceType} sci/unwanted-attrs))]
    (clojure.pprint/pprint (sci/strip-unwanted-attrs r unwanted))))

(defn print-resources
  []
  (init-db-client)
  (doseq [rname *resources*]
    (println "::: Resource:" rname)
    (let [r (get-resource rname)]
      (if (seq r)
        (print-resource r)
        (do
          (println "WARNING: Resource" rname "not found."))))))

(defn get-connector-resources
  []
  (let [cs (-> (con/query-impl (sci/connector-as-request configuration-resource))
               :body
               :connectors)]
    (map #(:id %) cs)))

(defn get-configuration-resources
  []
  (let [cs (-> (cfg/query-impl (sci/configuration-as-request connector-resource))
               :body
               :configurations)]
    (map #(:id %) cs)))

(defn get-persisted-resources
  [name]
  (cond
    (= name configuration-resource) (get-configuration-resources)
    (= name connector-resource) (get-connector-resources)
    :else (do (println "WARNING: Don't know how to get resources for" name))))

(defn list-persisted-resources
  [name]
  (doseq [r (get-persisted-resources name)]
    (println r)))

(defn list-resources
  []
  (init-db-client)
  (println "List of persisted resources.")
  (doseq [r resource-types]
    (println (format "- resources for '%s'" r))
    (list-persisted-resources r)))

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

(defn cli-parse-sets
  ([m k v]
   (cli-parse-sets m k v identity))
  ([m k v fun] (assoc m k (if-let [oldval (get m k)]
                            (merge oldval (fun v))
                            (hash-set (fun v))))))

(def cli-options
  [["-t" "--template TEMPLATE" "Prints out registered template by name."]
   ["-l" "--list" "Lists available templates."]
   ["-p" "--persisted" "Lists resources persisted in DB."]
   ["-r" "--resource RESOURCE" "Prints out persisted resource document(s) by name."
    :id :resources
    :default #{}
    :assoc-fn cli-parse-sets]
   ["-h" "--help"]])

(def prog-help
  "
  Ensure :id key with a corresponding resource name is present in each configuration.
  Connector configuration must contain :cloudServiceType key, which corresponds to
  the connector name.

  ES_HOST and ES_PORT should be exported in the environment for the utility
  to be able to connect to Elasticsearch.")

(defn usage
  [options-summary]
  (->> [""
        "Adds or updates different parts of the SlipStream service configuration in DB."
        ""
        "Usage: program-name [options] <list-of-files>"
        ""
        "Options:"
        options-summary
        ""
        "Arguments:"
        "  <list-of-files>    list of edn files with configurations."
        ""
        prog-help]
       (s/join \newline)))


(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (when (not (empty? arguments))
      (init-namespaces)
      (run arguments)
      (System/exit 0))
    (when (seq (:resources options))
      (alter-var-root #'*resources* (fn [_] (:resources options)))
      (print-resources)
      (System/exit 0))
    (when (:list options)
      (init-namespaces)
      (list-tempates)
      (System/exit 0))
    (when (not (s/blank? (:template options)))
      (init-namespaces)
      (print-template (:template options)))
    (when (:persisted options)
      (list-resources))))