(ns com.sixsq.slipstream.ssclj.resources.user-oidc-registration
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.internal :as internal]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.email.utils :as email-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-oidc-registration :as user-template-spec]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-oidc-registration :as user-template]
    [com.sixsq.slipstream.ssclj.resources.user.utils :as user-utils]))

;;
;; multimethods for validation
;;

(def create-validate-fn (u/create-spec-validation-fn ::user-template-spec/oidc-registration-create))
(defmethod p/create-validate-subtype user-template/registration-method
  [{resource :userTemplate :as create-document}]
  (create-validate-fn create-document))

;;
;; transform template into user resource
;; strips method attribute and updates the resource URI
;;

(def user-defaults {:resourceURI p/resource-uri
                    :isSuperUser false
                    :deleted     false
                    :state       "NEW"})


(defmethod p/tpl->user user-template/registration-method
  [{:keys [password] :as resource} request]
  (-> resource
      (merge user-defaults)
      (dissoc :instance)))


;;
;; creates email validation callback after user is created
;; logs and then ignores any exceptions when creating callback
;;

(defmethod p/post-user-add user-template/registration-method
  [{:keys [id emailAddress] :as resource} {:keys [base-uri] :as request}]
  (try
    (-> id
        (user-utils/create-user-email-callback base-uri)
        (email-utils/send-validation-email emailAddress))
    (catch Exception e
      (log/error (str e)))))
