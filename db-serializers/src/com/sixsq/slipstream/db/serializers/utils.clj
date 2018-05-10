(ns com.sixsq.slipstream.db.serializers.utils
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.db.loader :as db-loader]
    [environ.core :as env]))

(def default-db-binding-ns "com.sixsq.slipstream.db.es.loader")

(defn throw-on-resp-error
  [resp]
  (if (> (:status resp) 400)
    (let [msg (-> resp :body :message)]
      (throw (RuntimeException. msg (ex-info msg (:body resp)))))
    resp))

(defn warn-on-resp-error
  [resp]
  (if (> (:status resp) 400)
    (let [msg (-> resp :body :message)]
      (log/warn "Failed with: " msg ". " (:body resp)))
    resp))

(defn read-str
  [s]
  (if (string? s)
    (try
      (read-string s)
      (catch NumberFormatException _
        s)
      (catch RuntimeException ex
        (if-not (str/starts-with? (.getMessage ex) "Invalid token")
          (throw ex)
          s)))
    s))

(defn display
  [d & [msg]]
  (println "--->>> " (or msg ""))
  (clojure.pprint/pprint d)
  (println (or msg "") "<<<--- ")
  d)

(defn display->>
  [msg d]
  (println "--->>> " (or msg ""))
  (clojure.pprint/pprint d)
  (println (or msg "") "<<<--- ")
  d)

;;
;; DB related.
;;

(defn as-boolean
  [maybe-boolean]
  (if (string? maybe-boolean)
    (read-string maybe-boolean)
    maybe-boolean))

(defn not-empty-string?
  [x]
  (and (string? x) (not (empty? x))))

;;
;; Resource helpers.
;;

(defn as-request
  [body resource-uuid user-roles-str]
  (let [request {:params  {:uuid resource-uuid}
                 :body    (or body {})
                 :headers {aih/authn-info-header user-roles-str}}]
    ((aih/wrap-authn-info-header identity) request)))


;;
;; Parameters
;;

(defn param-get-cat-and-name
  [p]
  (str/split (.getName p) #"\." 2))

(defn param-get-pname
  "Get unqualified parameter name by removing its category."
  [p]
  (second (param-get-cat-and-name p)))

(defn qualified-pname
  [{:keys [name displayName] :as desc} category]
  (cond->> (or name displayName)
           category (str category ".")))

(defn build-sc-param
  [^Object serviceConf value desc & [category]]
  (when-let [name (qualified-pname desc category)]
    (let [scp (.buildServiceConfigParam serviceConf name (str value) (or (:description desc) ""))]
      (when desc
        (when (or category (:category desc))
          (.setCategory scp (or category (:category desc))))
        (when-not (nil? (:mandatory desc))
          (.setMandatory scp (as-boolean (:mandatory desc))))
        (when (:type desc)
          (.setType scp (str/capitalize (:type desc))))
        (when-not (nil? (:readOnly desc))
          (.setReadonly scp (as-boolean (get desc :readOnly (:readOnly desc)))))
        (when (:order desc)
          (.setOrder scp (read-str (:order desc))))
        (when (:instructions desc)
          (.setInstructions scp (:instructions desc)))
        (when-not (empty? (:enum desc))
          (.setEnumValues scp (:enum desc))))
      scp)))

(defn desc-from-param
  [p]
  (let [pd {:displayName (param-get-pname p)
            :type        (str/lower-case (.getType p))
            :category    (.getCategory p)
            :description (.getDescription p)
            :mandatory   (.isMandatory p)
            :readOnly    (.isReadonly p)
            :order       (.getOrder p)}]

    (cond-> pd
            (> (count (.getEnumValues p)) 0) (assoc :enum (vec (.getEnumValues p)))
            (not-empty-string? (.getInstructions p)) (assoc :instructions (.getInstructions p)))))


;;
;; Initializer
;;

(def dyn-init (delay (dyn/initialize)))

(defn initialize
  []
  @dyn-init)


;;
;; database binding initialization
;;

(defn init-db-binding
  "Load the binding for the persistent database implementation. Uses the
   environmental variables PERSISTENT_DB_BINDING_NS to load the binding. The
   chosen binding may also read configuration parameters from the environment.
   For example, the Elasticsearch bindings will use ES_HOST and ES_PORT."
  []
  (db-loader/load-and-set-persistent-db-binding
    (env/env :persistent-db-binding-ns default-db-binding-ns)))


