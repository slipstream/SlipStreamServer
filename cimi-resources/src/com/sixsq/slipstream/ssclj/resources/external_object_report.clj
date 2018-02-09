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
    [com.sixsq.slipstream.ssclj.util.log :as logu])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const objectType "report")

(def ExternalObjectReportDescription
  tpl/ExternalObjectTemplateReportDescription)

;;
;; description
;;
(def ^:const desc ExternalObjectReportDescription)

;; S3 pre-signed-URLs
(def ^:const report-bucket "slipstream-reports") ;;single bucket containing reports, must exist
(def ^:const default-ttl 15) ;; presigned URL time in mn before expiration

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
  [{state :state id :id :as resource} {{ttl :ttl} :body :as request}]
  (if (= state eo/state-new)
    (do
      (log/warn "Requesting upload url for report  : " id)
      (assoc resource :state eo/state-ready :uri (s3/generate-url report-bucket id (or ttl default-ttl) true)))
    (logu/log-and-throw-400 "Upload url request is not allowed")))

(defmethod eo/upload-subtype objectType
  [resource {{uuid :uuid} :params :as request}]
  (try
    (a/can-modify? resource request)
    (let [id (str (u/de-camelcase eo/resource-name) "/" uuid)]
      (-> (db/retrieve id request)
          (upload-fn request)
          (db/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))

;; Download URL request operation
(defn download-fn
  [{state :state id :id :as resource}  {{ttl :ttl} :body :as request}]
  (if (= state eo/state-ready)
    (do
      (log/warn "Requesting download url for report : " id)
      (assoc resource :uri (s3/generate-url report-bucket id (or ttl default-ttl))))
    (logu/log-and-throw-400 "Getting download  url request is not allowed")))

(defmethod eo/download-subtype objectType
  [resource {{uuid :uuid} :params :as request}]
  (try
    (a/can-modify? resource request)
    (let [id (str (u/de-camelcase eo/resource-name) "/" uuid)]
      (-> (db/retrieve id request)
          (download-fn request)
          (db/edit request)))
    (catch ExceptionInfo ei
      (ex-data ei))))


