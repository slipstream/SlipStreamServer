(ns com.sixsq.slipstream.ssclj.resources.external-object-report
  (:require
    [clj-time.core :as t]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as eot]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-template-report]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as conf-ss]))


(def ExternalObjectReportDescription
  eot/ExternalObjectTemplateReportDescription)

;;
;; description
;;
(def ^:const desc ExternalObjectReportDescription)

(def request-admin {:identity     {:current "internal"
                                   :authentications
                                            {"internal" {:roles #{"ADMIN"}, :identity "internal"}}}
                    :sixsq.slipstream.authn/claims
                                  {:username "internal", :roles "ADMIN"}
                    :params       {:resource-name "user"}
                    :route-params {:resource-name "user"}
                    :user-roles   #{"ANON"}})

(defn set-uuid-in-request
  [request uuid]
  (update-in request [:params] #(merge % {:uuid uuid})))

(defn ss-conf
  "Returns SlipStream configuration."
  []
  (let [request (set-uuid-in-request request-admin conf-ss/service)]
    (:body ((std-crud/retrieve-fn p/resource-url) request))))

(defn object-name
  [{:keys [runUUID filename]}]
  (format "%s/%s" runUUID filename))

(defmethod eo/tpl->externalObject eot/objectType
  [resource]
  (let [{:keys [reportsObjectStoreBucketName reportsObjectStoreCreds]} (ss-conf)]
    (-> resource
        (merge {:objectStoreCred {:href reportsObjectStoreCreds}
                :bucketName      reportsObjectStoreBucketName
                :objectName      (object-name resource)})
        (dissoc :filename))))

(defmethod eo/identity-for-creds eot/objectType
  [_ _]
  request-admin)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object.report))
(defmethod eo/validate-subtype eot/objectType
  [resource]
  (validate-fn resource))

