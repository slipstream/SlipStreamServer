(ns com.sixsq.slipstream.ssclj.resources.external-object-public
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-public :as eot]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-public :as eo-public]))


;; multimethods for validation

(def validate-fn (u/create-spec-validation-fn ::eo-public/external-object))
(defmethod eo/validate-subtype eot/objectType
  [resource]
  (validate-fn resource))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize eo/resource-url ::eo-public/external-object))

(defmethod eo/ready-subtype eot/objectType
  [{:keys [objectStoreCred bucketName objectName] :as resource} request]
  (let [s3client (-> objectStoreCred
                     (s3/format-creds-for-s3-api)
                     (s3/get-s3-client))]
  (-> resource
      (a/can-modify? request)
      (eo/verify-state #{eo/state-uploading} "ready")
      (s3/set-public-read-object)
      (s3/add-public-url)
      (assoc :state eo/state-ready)
      (s3/add-s3-size s3client bucketName objectName)
      (s3/add-s3-md5sum s3client bucketName objectName)
      (db/edit request))))