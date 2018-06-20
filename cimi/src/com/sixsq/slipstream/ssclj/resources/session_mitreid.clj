(ns com.sixsq.slipstream.ssclj.resources.session-mitreid
  (:require
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.ssclj.resources.callback-create-session-mitreid :as cb]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as oidc-utils]
    [com.sixsq.slipstream.ssclj.resources.session-template-mitreid :as tpl]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
    [com.sixsq.slipstream.ssclj.resources.spec.session :as session]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-mitreid :as session-tpl]))

(def ^:const authn-method "mitreid")

(def ^:const login-request-timeout (* 3 60))



;;
;; schemas
;;

(def SessionDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc SessionDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn ::session/session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn ::session-tpl/mitreid-create))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into session resource
;;
(defmethod p/tpl->session authn-method
  [{:keys [href redirectURI] :as resource} {:keys [headers base-uri] :as request}]
  (let [[client-id client-secret base-url public-key authorizeURL tokenURL userInfoURL] (oidc-utils/config-mitreid-params redirectURI (u/document-id href))]
    (if (or (and base-url client-id public-key) (and authorizeURL tokenURL client-id client-secret public-key))
      (let [session-init (cond-> {:href href}
                                 redirectURI (assoc :redirectURI redirectURI))
            session (sutils/create-session session-init headers authn-method)
            session (assoc session :expiry (ts/rfc822->iso8601 (ts/expiry-later-rfc822 login-request-timeout)))
            callback-url (oidc-utils/create-callback base-uri (:id session) cb/action-name)
            redirect-url (oidc-utils/create-redirect-url base-url authorizeURL client-id callback-url)]
        [{:status 303, :headers {"Location" redirect-url}} session])
      (oidc-utils/throw-bad-client-config authn-method redirectURI))))

;;
;; initialization: no schema for this parent resource
;;
(defn initialize
  []
  (std-crud/initialize p/resource-url ::session/session))
