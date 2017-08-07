(ns com.sixsq.slipstream.db.serializers.utils
  (:require
    [clojure.tools.logging :as log]

    [superstring.core :as s]

    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn])
  (:import
    (com.sixsq.slipstream.persistence ServiceConfigurationParameter)
    (com.sixsq.slipstream.persistence ParameterType)))

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
        (if-not (s/starts-with? (.getMessage ex) "Invalid token")
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

(defn init-test-es-client-and-db-impl
  "STRONGLY DEPRECATED. Use `with-test-es-client-and-db-impl` instead. Will
   initialize an Elasticsearch test client and set the database implementation.
   Because `.close` will never be called on the generated node and client, THIS
   FUNCTION WILL LEAK MEMORY."
  []
  (let [node (esu/create-test-node)
        client (-> node
                   esu/node-client
                   esb/wait-client-create-index)]
    (set! esb/*client* client)
    (db/set-impl! (esb/get-instance))))

(defmacro with-test-es-client-and-db-impl
  "Creates an Elasticsearch test client, executes the body with the created
   client bound to the Elasticsearch client binding, and then clean up the
   allocated resources by closing both the client and the node."
  [& body]
  `(with-open [node# (esu/create-test-node)
               client# (-> node#
                           esu/node-client
                           esb/wait-client-create-index)]
     (binding [esb/*client* client#]
       (db/set-impl! (esb/get-instance))
       (esu/reset-index esb/*client* esb/index-name)
       ~@body)))

(defn test-fixture-es-client-and-db-impl
  [f]
  (with-test-es-client-and-db-impl
    (f)))

(defn dump
  [resource & [msg]]
  (println "DB DUMP: " (or msg ""))
  (clojure.pprint/pprint (esu/dump esb/*client* esb/index-name resource)))

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
  (s/split (.getName p) #"\." 2))

(defn param-get-pname
  "Get unqualified parameter name by removing its category."
  [p]
  (second (param-get-cat-and-name p)))

(defn qualified-pname
  [desc category]
  (let [n (get desc :name (:displayName desc))]
    (if (not (nil? category))
      (str category "." n)
      n)))

(defn build-sc-param
  [value desc & [category]]
  (when-let [name (qualified-pname desc category)]
    (let
      [scp (ServiceConfigurationParameter. name
                                           (str value)
                                           (or (:description desc) ""))]
      (when desc
        (when (or category (:category desc))
          (.setCategory scp (or category (:category desc))))
        (when-not (nil? (:mandatory desc))
          (.setMandatory scp (as-boolean (:mandatory desc))))
        (when (:type desc)
          (.setType scp (ParameterType/valueOf (s/capitalize (:type desc)))))
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
            :type        (s/lower-case (.getType p))
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

(def dyn-init
  (delay
    (dyn/initialize)))

(defn initialize
  []
  @dyn-init)

