(ns com.sixsq.slipstream.ssclj.resources.run-parameter
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.run-parameter]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.acl :as a]
    [superstring.core :as str]
    [ring.util.response :as r]
    [com.sixsq.slipstream.util.response :as sr]
    [ring.sse :as sse]
    ))

(def ^:const resource-name "RunParameter")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "RunParameterCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})
;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-spec-validation-fn :cimi/run-parameter))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))
;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

;(def handler
;  (sse/event-channel-handler
;    (fn [request response raise event-ch]
;      (async/go
;        (dotimes [i 20]
;          (let [event {:id   (java.util.UUID/randomUUID)
;                       :name "foo"
;                       :data (json/generate-string {:foo "bar"})}]
;            (async/>! event-ch event)
;            (async/<! (async/timeout 1000))))
;        (async/close! event-ch)))
;    {:on-client-disconnect #(log/debug "sse/on-client-disconnect: " %)}))

;(defn retrieve-fn
;  [resource-name]
;  (fn [{{uuid :uuid} :params :as request}]
;    (try
;      (-> (str (u/de-camelcase resource-name) "/" uuid)
;          (db/retrieve request)
;          (a/can-view? request)
;          (crud/set-operations request)
;          (r/json-response))
;      (catch ExceptionInfo ei
;        (ex-data ei)))))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  ;(handler request (partial retrieve-fn resource-name) (fn[e] (log/error e)))
  (std-crud/retrieve-fn resource-name)
  )

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))
