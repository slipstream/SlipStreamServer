(ns com.sixsq.slipstream.ssclj.resources.callback-create-user-oidc
  "Creates a new OIDC user resource presumably after external authentication has succeeded."
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.oidc :as auth-oidc]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as oidc-utils]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "user-oidc-creation")


(defn register-user
  [{{href :href} :targetResource callback-id :id :as callback-resource} {:keys [headers base-uri uri] :as request}]
  (let [instance (u/document-id href)
        redirectURI nil                                     ;;FIXME
        [client-id base-url public-key] (oidc-utils/config-params redirectURI instance)
        ]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-oidc/get-oidc-access-token client-id base-url code (str base-uri (or callback-id "unknown-id") "/execute"))]
        (try
          (let [{:keys [sub email given_name family_name realm] :as claims} (sign/unsign-claims access-token public-key)
                roles (concat (oidc-utils/extract-roles claims)
                              (oidc-utils/extract-groups claims)
                              (oidc-utils/extract-entitlements claims))]
            (log/debug "oidc access token claims for" instance ":" (pr-str claims))
            (if sub
              (if-let [matched-user (ex/create-user-when-missing! {:authn-login  sub
                                                                   :email        email
                                                                   :firstname    given_name
                                                                   :lastname     family_name
                                                                   :organization realm})]
                  matched-user                              ;;FIXME : what should really be returned ?
                (oidc-utils/throw-inactive-user sub redirectURI))
              (oidc-utils/throw-no-subject redirectURI)))
          (catch Exception e
            (oidc-utils/throw-invalid-access-code (str e) redirectURI)))
        (oidc-utils/throw-no-access-token redirectURI))
      (oidc-utils/throw-missing-oidc-code redirectURI)
      )

    ))

(defmethod callback/execute action-name
  [{callback-id :id :as callback-resource} request]
  (log/debug "Executing callback" callback-id)
  (try
    (if-let [resp (register-user callback-resource request)]
      resp                                                  ;; FIXME : not sure what is returned here
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not create OIDC user" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))

