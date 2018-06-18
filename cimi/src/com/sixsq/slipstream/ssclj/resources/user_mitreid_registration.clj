(ns com.sixsq.slipstream.ssclj.resources.user-mitreid-registration
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.email.utils :as email-utils]
    [com.sixsq.slipstream.ssclj.resources.session-mitreid.utils :as mitreid-utils]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-mitreid-registration :as user-template-spec]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-mitreid-registration :as user-template]
    [com.sixsq.slipstream.ssclj.resources.user.utils :as user-utils]))

;;
;; multimethods for validation
;;

(def create-validate-fn (u/create-spec-validation-fn ::user-template-spec/mitreid-registration-create))
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
  [{:keys [href redirectURI] :as resource} {:keys [headers base-uri] :as request}]
  (let [[client-id client-secret base-url public-key authorizeURL tokenURL] (mitreid-utils/config-params redirectURI (u/document-id href))]
    (if (or (and base-url client-id public-key) (and authorizeURL tokenURL client-id client-secret public-key))
      (let [data (when redirectURI {:redirectURI redirectURI})
            callback-url (user-utils/create-user-mitreid-callback base-uri href data)
            redirect-url (mitreid-utils/create-redirect-url base-url authorizeURL client-id callback-url)]
        [{:status 303, :headers {"Location" redirect-url}} nil])
      (mitreid-utils/throw-bad-client-config user-template/registration-method redirectURI))))



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
