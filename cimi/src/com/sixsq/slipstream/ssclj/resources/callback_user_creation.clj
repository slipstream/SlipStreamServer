(ns com.sixsq.slipstream.ssclj.resources.callback-user-creation
  "Creates a new user resource presumably after some external authentication
   method has succeeded."
  (:require
    [com.sixsq.slipstream.auth.external :as external]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [com.sixsq.slipstream.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as oidc-utils]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.cyclone :as auth-oidc]
    [com.sixsq.slipstream.ssclj.resources.session.utils :as sutils]
    [com.sixsq.slipstream.auth.utils.sign :as sign]
    [clojure.tools.logging :as log]))


(def ^:const action-name "user-creation")


(defn create-user
  [request]
  (let [redirectURI "some-fake-uri"
        instance "oidc"
        [client-id base-url public-key] (oidc-utils/config-params nil instance)]
    (if-let [code (uh/param-value request :code)]
      (if-let [access-token (auth-oidc/get-oidc-access-token client-id base-url code "same-fake-uri-2")]
        (try
          (let [{:keys [sub email] :as claims} (sign/unsign-claims access-token public-key)
                user-info {:authn-login       sub
                           :authn-method      "github"
                           :email             email
                           :fail-on-existing? true}]
            (log/debug "oidc access token claims for" instance ":" (pr-str claims))
            (if sub
              (try
                (let [created-user (external/create-user-when-missing! user-info)]
                  (some->> created-user (str "user/")))
                (catch Exception e
                  (oidc-utils/throw-cannot-create-user sub redirectURI)))
              (oidc-utils/throw-no-subject redirectURI)))
          (catch Exception e
            (oidc-utils/throw-invalid-access-code (str e) redirectURI)))
        (oidc-utils/throw-no-access-token redirectURI))
      (oidc-utils/throw-missing-oidc-code redirectURI))))


(defmethod callback/execute action-name
  [{callback-id :id :as callback-resource} request]
  (try
    (if-let [user-id (create-user request)]
      (do
        (utils/callback-succeeded! callback-id)
        (r/map-response "success" 200 user-id))
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not register OIDC user" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))
