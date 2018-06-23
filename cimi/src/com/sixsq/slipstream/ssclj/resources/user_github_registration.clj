(ns com.sixsq.slipstream.ssclj.resources.user-github-registration
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.email.utils :as email-utils]
    [com.sixsq.slipstream.ssclj.resources.github.utils :as gu]
    [com.sixsq.slipstream.ssclj.resources.spec.user]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-github-registration :as user-template-spec]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-github-registration :as user-template]
    [com.sixsq.slipstream.ssclj.resources.user.utils :as user-utils]))

;;
;; multimethods for validation
;;

(def create-validate-fn (u/create-spec-validation-fn ::user-template-spec/github-registration-create))
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
  (let [[client-id clientSecret] (gu/config-github-params redirectURI (u/document-id href))]
    (if (and client-id clientSecret)
      (let [data (when redirectURI {:redirectURI redirectURI})
            callback-url (user-utils/create-user-github-callback base-uri href data)
            redirect-url (format gu/github-oath-endpoint client-id callback-url)]
        [{:status 303, :headers {"Location" redirect-url}} nil])
      (gu/throw-bad-client-config user-template/registration-method redirectURI))))





(defmethod p/post-user-add user-template/registration-method
  [resource request]
  nil ;; FIXME
  )
