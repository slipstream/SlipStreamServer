(ns com.sixsq.slipstream.ssclj.resources.credential-template-cloud-dummy
  "This CredentialTemplate allows creating a Cloud Credential instance to hold
  cloud credentials for Dummy cloud."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.connector.dummy-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud-dummy]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.userparamsdesc :refer [slurp-cloud-cred-desc]]))

(def ^:const credential-type (str "cloud-cred-" ct/cloud-service-type))
(def ^:const method (str "store-cloud-cred-" ct/cloud-service-type))

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;
(def ^:const resource
  {:type        credential-type
   :method      method
   :name        "Dummy cloud credentials store"
   :description "Stores user cloud credentials for Dummy"
   :quota       20
   :connector   {:href ""}
   :key         ""
   :secret      ""
   :domain-name ""
   :acl         resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/CredentialTemplateDescription
         (slurp-cloud-cred-desc ct/cloud-service-type)))

;;
;; initialization: register this Credential template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential-template.cloud-dummy))
(defmethod p/validate-subtype method
  [resource]
  (validate-fn resource))
