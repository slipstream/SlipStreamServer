(ns com.sixsq.slipstream.ssclj.resources.callback-create-user-github
  "Creates a new GitHub user resource after external authentication has succeeded."
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.external :as ex]
    [com.sixsq.slipstream.auth.github :as auth-github]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.github.utils :as gu]
    [com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils :as uiu]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "user-github-creation")


(defn register-user
  [{{:keys [href]} :targetResource {:keys [redirectURI]} :data :as callback-resource} request]
  (let [{:keys [instance]} (crud/retrieve-by-id-as-admin href)
        [client-id client-secret] (gu/config-github-params redirectURI instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-github/get-github-access-token client-id client-secret code)]
        (if-let [user-info (auth-github/get-github-user-info access-token)]
          (do
            (log/debugf "github user info for %s: %s" instance (str user-info))
            (let [github-login (:login user-info)
                  github-email (auth-github/retrieve-email user-info access-token)]
              (if github-login
                (if-let [matched-user (ex/create-user-when-missing! :github {:external-login    github-login
                                                                             :external-email    github-email
                                                                             :fail-on-existing? true})]
                  (do
                    (uiu/add-user-identifier! matched-user :github github-login nil)
                    matched-user)
                  (gu/throw-user-exists github-login redirectURI))
                (gu/throw-no-matched-user redirectURI))))
          (gu/throw-no-user-info redirectURI))
        (gu/throw-no-access-token redirectURI))
      (gu/throw-missing-oauth-code redirectURI))))


(defmethod callback/execute action-name
  [{callback-id :id {:keys [redirectURI]} :data :as callback-resource} request]
  (log/debug "Executing callback" callback-id)
  (try
    (if-let [username (register-user callback-resource request)]
      (do
        (utils/callback-succeeded! callback-id)
        (if redirectURI
          (r/map-response (format "user '%s' created" username) 303 callback-id (or redirectURI "/"))
          (r/map-response (format "user '%s' created" username) 201)))
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not create github user" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))
