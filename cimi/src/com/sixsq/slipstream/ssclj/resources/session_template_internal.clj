(ns com.sixsq.slipstream.ssclj.resources.session-template-internal
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal :as session-tpl]))


(def ^:const authn-method "internal")


(def default-template {:method      authn-method
                       :instance    authn-method
                       :name        "Internal"
                       :description "Internal Authentication via Username/Password"
                       :group       "Login with Username/Password"
                       :username    "username"
                       :password    "password"
                       :acl         p/resource-acl})


;;
;; description
;;

(def ^:const desc
  (merge p/SessionTemplateDescription
         {:username {:displayName  "Username"
                     :category     "general"
                     :description  "username"
                     :type         "string"
                     :mandatory    true
                     :readOnly     false
                     :order        20
                     :autocomplete "username"}
          :password {:displayName  "Password"
                     :category     "general"
                     :description  "password"
                     :type         "password"
                     :mandatory    true
                     :readOnly     false
                     :order        21
                     :autocomplete "password"}}))


;;
;; initialization: register this Session template and create internal authentication template
;;

(defn initialize
  []
  (p/register authn-method desc)
  (std-crud/initialize p/resource-url ::session-tpl/internal)
  (std-crud/add-if-absent (str "session-template/" authn-method) p/resource-url default-template))


;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::session-tpl/internal))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
