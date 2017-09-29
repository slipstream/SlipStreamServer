(ns com.sixsq.slipstream.ssclj.resources.deployment-template-std
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.deployment-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template-std]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const method "standard")

(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ADMIN"
                            :type      "ROLE"
                            :right     "VIEW"}
                           {:principal "USER"
                            :type      "ROLE"
                            :right     "VIEW"}
                           ]})

;;
;; resource
;;
(def ^:const resource
  {:method      method
   :name        "Standard deployment"
   :description "Direct creation of deployment"
   :module      "module"
   :acl         resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/DeploymentTemplateDescription
         {:module {:displayName "Module"
                   :category    "general"
                   :description "module"
                   :type        "string"
                   :mandatory   true
                   :readOnly    false
                   :order       20}}))

;;
;; initialization: register this User template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/deployment-template.std))
(defmethod p/validate-subtype method
  [resource]
  (validate-fn resource))
