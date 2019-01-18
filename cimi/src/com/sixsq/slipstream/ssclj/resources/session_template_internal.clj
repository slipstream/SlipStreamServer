(ns com.sixsq.slipstream.ssclj.resources.session-template-internal
  "
Resource that is used to create a session using a username and password for
credentials. This template is guaranteed to be present on all server instances.
"
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as md]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal :as session-tpl]
    [com.sixsq.slipstream.ssclj.util.metadata :as gen-md]))


(def ^:const authn-method "internal")


(def ^:const resource-name "Internal")


(def ^:const resource-url authn-method)


(def default-template {:method           authn-method
                       :instance         authn-method
                       :name             "Internal"
                       :description      "Internal Authentication via Username/Password"
                       :resourceMetadata (str p/resource-url "-" authn-method)
                       :group            "Login with Username/Password"
                       :username         "username"
                       :password         "password"
                       :acl              p/resource-acl})


;;
;; initialization: register this Session template and create internal authentication template
;;

(defn initialize
  []
  (std-crud/initialize p/resource-url ::session-tpl/internal)
  (std-crud/add-if-absent (str "session-template/" authn-method) p/resource-url default-template)
  (md/register (gen-md/generate-metadata ::ns ::p/ns ::session-tpl/internal)))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::session-tpl/internal))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
