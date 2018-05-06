(ns com.sixsq.slipstream.db.serializers.utils
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.db.es.common.utils :as escu]
    [com.sixsq.slipstream.db.es.utils :as esu]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [superstring.core :as s]))

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

#_(defn dump
  [resource & [msg]]
  (println "DB DUMP: " (or msg ""))
  (let [index-name (escu/id->index resource)]
    (clojure.pprint/pprint (esu/dump esb/*client* index-name resource))))

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
  [^Object serviceConf value desc & [category]]
  (when-let [name (qualified-pname desc category)]
    (let [scp (.buildServiceConfigParam serviceConf name (str value) (or (:description desc) ""))]
      (when desc
        (when (or category (:category desc))
          (.setCategory scp (or category (:category desc))))
        (when-not (nil? (:mandatory desc))
          (.setMandatory scp (as-boolean (:mandatory desc))))
        (when (:type desc)
          (.setType scp (s/capitalize (:type desc))))
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
