(ns com.sixsq.slipstream.ssclj.resources.user-template-mitreid
  "
Resource that is used to create a user account from the standard OIDC
authentication workflow as implemented by a MITREid server.
"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as md]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-mitreid :as mitreid]
    [com.sixsq.slipstream.ssclj.resources.user-template :as p]
    [com.sixsq.slipstream.ssclj.util.metadata :as gen-md]))


(def ^:const registration-method "mitreid")


(def ^:const resource-url registration-method)


(def resource-acl {:owner {:principal "ADMIN"
                           :type      "ROLE"}
                   :rules [{:principal "ANON"
                            :type      "ROLE"
                            :right     "VIEW"}]})

;;
;; resource
;;

(def ^:const resource
  {:method         registration-method
   :instance       registration-method
   :name           "MITREid Registration"
   :description    "Creates a new user through mitreid-registration"
   :acl            resource-acl})


;;
;; description
;;

(def ^:const desc p/UserTemplateDescription)


;;
;; initialization: register this User template
;;

(defn initialize
  []
  (p/register registration-method desc)
  (md/register (gen-md/generate-metadata ::ns ::p/ns ::mitreid/mitreid-registration)))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::mitreid/mitreid-registration))
(defmethod p/validate-subtype registration-method
  [resource]
  (validate-fn resource))