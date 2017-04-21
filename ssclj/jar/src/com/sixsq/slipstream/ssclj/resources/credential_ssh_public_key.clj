(ns com.sixsq.slipstream.ssclj.resources.credential-ssh-public-key
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.credential-ssh-public-key]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential :as p]
    [com.sixsq.slipstream.ssclj.resources.credential-template-ssh-public-key :as tpl]))

(def ConfigurationDescription
  tpl/desc)

;;
;; description
;; FIXME: Provide complete description and make visible through the API.
;;
(def ^:const desc ConfigurationDescription)

;;
;; convert template to credential
;; for this credential, this is just a copy of the template
;;
(defmethod p/tpl->credential tpl/credential-type
  [resource request]
  (assoc resource :resourceURI p/resource-uri))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/credential-template.ssh-public-key))
(defmethod p/validate-subtype tpl/credential-type
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/credential-template.ssh-public-key-create))
(defmethod p/create-validate-subtype tpl/credential-type
  [resource]
  (create-validate-fn resource))
