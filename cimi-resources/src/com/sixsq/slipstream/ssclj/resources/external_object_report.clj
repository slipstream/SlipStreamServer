(ns com.sixsq.slipstream.ssclj.resources.external-object-report
  (:require
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.auth.acl :as a]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.util.response :as r])
  (:import (clojure.lang ExceptionInfo)))


(def ^:const objectType "report")


(def ExternalObjectReportDescription
  tpl/ExternalObjectTemplateReportDescription)

;;
;; description
;;
(def ^:const desc ExternalObjectReportDescription)

(def ^:const reports-bucket-default "slipstream-reports")

;; single bucket containing reports, must exist
(def reports-bucket (delay (get (s3/object-store-config) :reportsBucketName reports-bucket-default)))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.report))
(defmethod eo/validate-subtype objectType
  [resource]
  (validate-fn resource))


(def create-validate-fn (u/create-spec-validation-fn :cimi/external-object-template.report-create))
(defmethod eo/create-validate-subtype objectType
  [resource]
  (create-validate-fn resource))


;; Upload URL request operation
(defn upload-fn
  [{:keys [id state contentType] :as resource} {{ttl :ttl} :body :as request}]
  (let [report-id (cu/document-id id)]
    (if (= state eo/state-new)
      (do
        (log/info "Requesting upload url for report:" report-id)
        (s3/generate-url @reports-bucket report-id :put (or ttl s3/default-ttl) contentType))
      (logu/log-and-throw-400 "Report object is not in new state to be uploaded!"))))


(defmethod eo/upload-subtype objectType
  [resource {{uuid :uuid} :params :as request}]
  (try
    (a/can-modify? resource request)
    (let [upload-uri (upload-fn resource request)]
      (-> (assoc resource :state eo/state-ready)
          (db/edit request))
      (r/json-response {:uri upload-uri}))
    (catch ExceptionInfo ei
      (ex-data ei))))


;; Download URL request operation
(defn download-fn
  [{state :state id :id :as resource} {{ttl :ttl} :body :as request}]
  (let [report-id (cu/document-id id)]
    (if (= state eo/state-ready)
      (do
        (log/info "Requesting download url for report : " report-id)
        (s3/generate-url @reports-bucket report-id :get (or ttl s3/default-ttl)))
      (logu/log-and-throw-400 "Report object is not in ready state to be downloaded!"))))


(defmethod eo/download-subtype objectType
  [resource {{uuid :uuid} :params :as request}]
  (try
    (a/can-modify? resource request)
    (r/json-response {:uri (download-fn resource request)})
    (catch ExceptionInfo ei
      (ex-data ei))))


(def delete-impl (std-crud/delete-fn eo/resource-name))


(defmethod eo/delete-subtype objectType
  [{id :id :as resource} {{keep? :keep-s3-object} :body :as request}]
  (let [keyname (cu/document-id id)]
    (when-not keep?
      (s3/delete-s3-object @reports-bucket keyname))         ;; delete the S3 object
    (delete-impl request)))
