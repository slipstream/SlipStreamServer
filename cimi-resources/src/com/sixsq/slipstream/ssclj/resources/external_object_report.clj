(ns com.sixsq.slipstream.ssclj.resources.external-object-report
  (:require
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object-template-report :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.spec.external-object-report]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-slipstream :as conf-ss])
  (:import (clojure.lang ExceptionInfo)))


(def ExternalObjectReportDescription
  tpl/ExternalObjectTemplateReportDescription)

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

(defmethod eo/tpl->externalObject tpl/objectType
  [resource]
  (let [{:keys [reportsObjectStoreBucketName reportsObjectStoreCreds]} (ss-conf)]
    (merge resource {:objectStoreCred {:href reportsObjectStoreCreds}
                     :bucketName      reportsObjectStoreBucketName})))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/external-object-template.report))
(defmethod eo/validate-subtype tpl/objectType
  [resource]
  (validate-fn resource))


(def create-validate-fn (u/create-spec-validation-fn :cimi/external-object-template.report-create))
(defmethod eo/create-validate-subtype tpl/objectType
  [resource]
  (create-validate-fn resource))
