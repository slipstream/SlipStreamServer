(ns com.sixsq.slipstream.ssclj.resources.callback-create-session-mitreid
  "Creates a new MITREid session resource presumably after external authentication has succeeded."
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.oidc :as auth-oidc]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as oidc-utils]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "session-mitreid-creation")


(defn validate-session
  [{{session-id :href} :targetResource callback-id :id :as callback-resource} {:keys [headers base-uri uri] :as request}]

  (let [{:keys [server clientIP redirectURI] {:keys [href]} :sessionTemplate :as current-session} (sutils/retrieve-session-by-id session-id)
        instance (u/document-id href)
        [client-id client-secret base-url public-key authorizeURL tokenURL userInfoURL] (oidc-utils/config-mitreid-params redirectURI instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-oidc/get-access-token client-id client-secret base-url tokenURL code (str base-uri (or callback-id "unknown-id") "/execute"))]
        (try
          (let [{:keys [sub] :as claims} (sign/unsign-claims access-token public-key)
                roles (concat (oidc-utils/extract-roles claims)
                              (oidc-utils/extract-groups claims)
                              (oidc-utils/extract-entitlements claims))
                {:keys [username] :as userinfo} (when sub (oidc-utils/get-mitreid-userinfo userInfoURL access-token))]
            (log/debug "MITREid access token claims for" instance ":" (pr-str claims))
            (if sub
              (if-let [matched-user (ex/match-oidc-username username instance)]
                (let [claims (cond-> (auth-internal/create-claims matched-user)
                                     session-id (assoc :session session-id)
                                     session-id (update :roles #(str session-id " " %))
                                     roles (update :roles #(str % " " (str/join " " roles)))
                                     server (assoc :server server)
                                     clientIP (assoc :clientIP clientIP))
                      cookie (cookies/claims-cookie claims)
                      expires (ts/rfc822->iso8601 (:expires cookie))
                      claims-roles (:roles claims)
                      updated-session (cond-> (assoc current-session :username matched-user :expiry expires)
                                              claims-roles (assoc :roles claims-roles))
                      {:keys [status] :as resp} (sutils/update-session session-id updated-session)]
                  (log/debug "MITREid cookie token claims for" instance ":" (pr-str claims))
                  (if (not= status 200)
                    resp
                    (let [cookie-tuple [(sutils/cookie-name session-id) cookie]]
                      (if redirectURI
                        (r/response-final-redirect redirectURI cookie-tuple)
                        (r/response-created session-id cookie-tuple)))))
                (oidc-utils/throw-inactive-user sub redirectURI))
              (oidc-utils/throw-no-subject redirectURI)))
          (catch Exception e
            (oidc-utils/throw-invalid-access-code (str e) redirectURI)))
        (oidc-utils/throw-no-access-token redirectURI))
      (oidc-utils/throw-missing-code redirectURI))))


(defmethod callback/execute action-name
  [{callback-id :id :as callback-resource} request]
  (log/debug "Executing callback" callback-id)
  (try
    (if-let [resp (validate-session callback-resource request)]
      resp
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not validate MITREid session" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))

