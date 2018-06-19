(ns com.sixsq.slipstream.ssclj.resources.callback-create-user-mitreid
  "Creates a new MITREid user resource presumably after external authentication has succeeded."
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.mitreid :as auth-mitreid]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-mitreid.utils :as mitreid-utils]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "user-mitreid-creation")


(defn register-user
  [{{href :href} :targetResource {:keys [redirectURI]} :data callback-id :id :as callback-resource} {:keys [headers base-uri uri] :as request}]
  (let [instance (u/document-id href)
        [client-id client-secret base-url public-key authorizeURL tokenURL userInfoURL] (mitreid-utils/config-params redirectURI instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-mitreid/get-mitreid-access-token client-id client-secret base-url tokenURL code (str base-uri (or callback-id "unknown-id") "/execute"))]
        (try
          (let [{:keys [sub] :as claims}  (sign/unsign-claims access-token public-key)
                 {:keys [username givenName familyName emails] :as userinfo} (when sub (auth-mitreid/get-mitreid-userinfo userInfoURL access-token))
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
                matched-user
                (mitreid-utils/throw-user-exists sub redirectURI))
              (mitreid-utils/throw-no-subject redirectURI)))
          (catch Exception e
            (mitreid-utils/throw-invalid-access-code (str e) redirectURI)))
        (mitreid-utils/throw-no-access-token redirectURI))
      (mitreid-utils/throw-missing-mitreid-code redirectURI))))


(defmethod callback/execute action-name
  [{callback-id :id :as callback-resource} request]
  (log/debug "Executing callback" callback-id)
  (try
    (if-let [username (register-user callback-resource request)]
      (do
        (utils/callback-succeeded! callback-id)
        (r/map-response (format "user '%s' created" username) 201))
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not create MITREid user" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))
