(ns com.sixsq.slipstream.db.serializers.utils
  (:require
    [superstring.core :as s]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter]
    [com.sixsq.slipstream.persistence ParameterType]
    ))


(defn throw-on-resp-error
  [resp]
  (if (> (:status resp) 400)
    (let [msg (-> resp :body :message)]
      (throw (RuntimeException. msg (ex-info msg (:body resp)))))
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

;;
;; DB related.
;;

;; Implementation of CRUD actions over DB.

(defn set-db-crud-impl-uncond
  []
  (db/set-impl! (esb/get-instance)))

(defn set-db-crud-impl
  []
  (if (instance? clojure.lang.Var$Unbound db/*impl*)
    (set-db-crud-impl-uncond)))

;; Connection to a remote ES.

(defn create-and-set-es-client
  "Connection to a remote ES.
  Requries ES_HOST and ES_PORT env vars."
  []
  (esb/set-client! (esb/create-client)))

(defn db-client-and-crud-impl
  "Sets up connection to a remote DB and sets DB CRUD implementation."
  []
  (set-db-crud-impl)
  (create-and-set-es-client))

;; Local test ES node.

(defn create-test-es-db-uncond
  []
  (esb/set-client! (esb/create-test-client)))

(defn create-test-es-db
  []
  (if (instance? clojure.lang.Var$Unbound esb/*client*)
    (create-test-es-db-uncond)))

(defn test-db-client-and-crud-impl
  []
  (set-db-crud-impl)
  (create-test-es-db))

(defn dump
  [resource & [msg]]
  (println "DB DUMP: " (or msg ""))
  (clojure.pprint/pprint (esu/dump esb/*client* esb/index-name resource)))

(defn as-boolean
  [maybe-boolean]
  (if (string? maybe-boolean)
    (read-string maybe-boolean)
    maybe-boolean))


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

(defn build-sc-param
  [value desc]
  (let [name (get desc :name (:displayName desc))
        scp (ServiceConfigurationParameter. name
                                            (str value)
                                            (:description desc))]
    (.setCategory scp (:category desc))
    (.setMandatory scp (as-boolean (:mandatory desc)))
    (.setType scp (ParameterType/valueOf (s/capitalize (:type desc))))
    (.setReadonly scp (as-boolean (get desc :readOnly (:readonly desc))))
    (.setOrder scp (read-str (:order desc)))
    (if (:instructions desc) (.setInstructions scp (:instructions desc)))
    (if-not (empty? (:enum desc)) (.setEnumValues scp (:enum desc)))
    scp))

(defn desc-from-param
  [p]
  (let [pd {:displayName (.getName p)
            :type        (s/lower-case (.getType p))
            :category    (.getCategory p)
            :description (.getDescription p)
            :mandatory   (.isMandatory p)
            :readOnly    (.isReadonly p)
            :order       (.getOrder p)
            }]
    (cond-> pd
            (.getEnumValues p) (assoc :enum (.getEnumValues p))
            (.getInstructions p) (assoc :instructions (.getInstructions p)))))


