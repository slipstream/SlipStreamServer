(ns com.sixsq.slipstream.ssclj.resources.callback-create-session-github
  "Creates a new Github session resource presumably after  external authentication has succeeded."
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.cookies :as cookies]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.github :as auth-github]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.timestamp :as ts]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "session-github-creation")


(defn validate-session
  [{:keys [headers uri redirectURI] :as request} session-id]
  (let [{:keys [server clientIP redirectURI] {:keys [href]} :sessionTemplate :as current-session} (sutils/retrieve-session-by-id session-id)
        instance (u/document-id href)
        [client-id client-secret] (sutils/config-github-params redirectURI instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-github/get-github-access-token client-id client-secret code)]
        (if-let [{:keys [user email] :as user-info} (auth-github/get-github-user-info access-token)]
          (do
            (log/debug "github user info for" instance ":" user-info)
            (let [external-login (auth-github/sanitized-login user-info)
                  external-email (auth-github/retrieve-email user-info access-token)
                  [matched-user _] (ex/match-existing-external-user :githublogin external-login external-email)]
              (if matched-user
                (let [claims (cond-> (auth-internal/create-claims matched-user)
                                     session-id (assoc :session session-id)
                                     session-id (update :roles #(str session-id " " %))
                                     server (assoc :server server)
                                     clientIP (assoc :clientIP clientIP))
                      cookie (cookies/claims-cookie claims)
                      expires (ts/rfc822->iso8601 (:expires cookie))
                      claims-roles (:roles claims)
                      updated-session (cond-> (assoc current-session :username matched-user :expiry expires)
                                              claims-roles (assoc :roles claims-roles))
                      {:keys [status] :as resp} (sutils/update-session session-id updated-session)]
                  (log/debug "github cookie token claims for" instance ":" claims)
                  (if (not= status 200)
                    resp
                    (let [cookie-tuple [(sutils/cookie-name session-id) cookie]]
                      (if redirectURI
                        (r/response-final-redirect redirectURI cookie-tuple)
                        (r/response-created session-id cookie-tuple)))))
                (sutils/throw-no-matched-user redirectURI))))
          (sutils/throw-no-user-info redirectURI))
        (sutils/throw-no-access-token redirectURI))
      (sutils/throw-missing-oauth-code redirectURI))))

(defmethod callback/execute action-name
  [{callback-id :id {session-id :href} :targetResource :as callback-resource} request]
  (try
    (if-let [resp (validate-session request session-id)]
      resp
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not validate github session" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))

