(ns com.sixsq.slipstream.auth.cyclone
  "
  # Decryption of claims in the cookie.

  The full path to the public key to decrypt the claims in the CYCLONE cookie
  is expected as one of
  - system environment variable `AUTHN_PUBLIC_KEY_CYCLONE`
  - system property `authn-public-key-cyclone`.
  "
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clj-http.client :as http]
            [com.sixsq.slipstream.auth.utils.config :as cf]
            [com.sixsq.slipstream.auth.utils.http :as uh]
            [com.sixsq.slipstream.auth.utils.sign :as sign]
            [com.sixsq.slipstream.auth.external :as ex]))

(def ^:private cyclone-base-url
  "https://federation.cyclone-project.eu/auth/realms/master/protocol/openid-connect")


(defn- redirect_uri
  []
  (str (cf/mandatory-property-value :auth-server) "/auth/callback-cyclone"))


(defn login-name
  [claims]
  (->> claims
       ((juxt :name :preferred_username))
       (remove empty?)
       first
       ex/sanitize-login-name))


(defn get-oidc-access-token
  [oidc-client-id oidc-base-url oidc-code redirect-uri]
  (-> (http/post (str oidc-base-url "/token")
                 {:headers     {"Accept" "application/json"}
                  :form-params {:grant_type   "authorization_code"
                                :code         oidc-code
                                :redirect_uri redirect-uri
                                :client_id    oidc-client-id}})
      :body
      (json/read-str :key-fn keyword)
      :access_token))


(defn callback-cyclone
  [request redirect-server]
  (try
    (let [code (uh/param-value request :code)

          access-token (get-oidc-access-token "slipstream" cyclone-base-url code (redirect_uri))

          claims (sign/unsign-claims access-token
                                     :auth-public-key-cyclone)]
      (log/debug "Cyclone claims " claims)
      (log/info "Successful CYCLONE login: " (login-name claims))
      (ex/redirect-with-matched-user :cyclone (login-name claims) (:email claims) redirect-server))
    (catch Exception e
      (log/error "Invalid Cyclone authentication " e)
      (uh/response-redirect (str redirect-server "/dashboard")))))
