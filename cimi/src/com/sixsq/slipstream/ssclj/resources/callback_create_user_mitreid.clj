(ns com.sixsq.slipstream.ssclj.resources.callback-create-user-mitreid
  "Creates a new MITREid user resource presumably after external authentication has succeeded."
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.oidc :as auth-oidc]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as oidc-utils]
    [com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils :as uiu]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const action-name "user-mitreid-creation")

(defn register-user
  [{{href :href} :targetResource {:keys [redirectURI]} :data callback-id :id :as callback-resource} {:keys [headers base-uri uri] :as request}]
  (let [instance (u/document-id href)
        [client-id client-secret public-key authorizeURL tokenURL userProfileURL] (oidc-utils/config-mitreid-params redirectURI instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-oidc/get-access-token client-id client-secret tokenURL code (str base-uri (or callback-id "unknown-id") "/execute"))]
        (try
          (let [{:keys [sub] :as claims} (sign/unsign-claims access-token public-key)
                {:keys [username givenName familyName emails] :as userinfo} (when sub (oidc-utils/get-mitreid-userinfo userProfileURL access-token))
                email (-> (filter :primary emails)
                          first
                          :value)]
            (log/debugf "MITREid access token claims for %s: %s" instance (pr-str claims))
            (if sub
              (if-let [matched-user (ex/create-user-when-missing! :mitreid {:external-login    username
                                                                            :external-email    (or email (str username "@fake-email.com"))
                                                                            :firstname         givenName
                                                                            :lastname          familyName
                                                                            :instance          instance
                                                                            :fail-on-existing? true})]
                (do
                  (uiu/add-user-identifier! matched-user :mitreid username instance)
                  matched-user)
                (oidc-utils/throw-user-exists sub redirectURI))
              (oidc-utils/throw-no-subject redirectURI)))
          (catch Exception e
            (oidc-utils/throw-invalid-access-code (str e) redirectURI)))
        (oidc-utils/throw-no-access-token redirectURI))
      (oidc-utils/throw-missing-code redirectURI))))


(defmethod callback/execute action-name
  [{callback-id :id {:keys [redirectURI]} :data :as callback-resource} request]
  (log/debug "Executing callback" callback-id)
  (try
    (if-let [username (register-user callback-resource request)]
      (do
        (utils/callback-succeeded! callback-id)
        (if redirectURI
          (r/map-response (format "user '%s' created" username) 303 callback-id redirectURI)
          (r/map-response (format "user '%s' created" username) 201)))
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not create MITREid user" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))

