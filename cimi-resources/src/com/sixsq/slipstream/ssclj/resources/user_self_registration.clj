(ns com.sixsq.slipstream.ssclj.resources.user-self-registration
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user.utils :as user-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.user-template-self-registration :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.auth.internal :as internal]
    [com.sixsq.slipstream.ssclj.resources.email.utils :as email-utils]))

;;
;; multimethods for validation
;;

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.self-registration-create))
(defmethod p/create-validate-subtype tpl/registration-method
  [{resource :userTemplate :as create-document}]
  (user-utils/check-password-constraints resource)
  (create-validate-fn create-document))

;;
;; transform template into user resource
;; strips method attribute and updates the resource URI
;;

(def user-defaults {:resourceURI p/resource-uri
                    :isSuperUser false
                    :deleted     false
                    :state       "NEW"})


(defmethod p/tpl->user tpl/registration-method
  [{:keys [password] :as resource} request]
  (-> resource
      (merge user-defaults)
      (dissoc :passwordRepeat)
      (assoc :password (internal/hash-password password))))


;;
;; creates email validation callback after user is created
;; logs and then ignores any exceptions when creating callback
;;

(defmethod p/post-user-add tpl/registration-method
  [{:keys [id emailAddress] :as resource} {:keys [baseURI] :as request}]
  (try
    (-> id
        (user-utils/create-callback baseURI)
        (email-utils/send-validation-email emailAddress))
    (catch Exception e
      (log/error (str e)))))
