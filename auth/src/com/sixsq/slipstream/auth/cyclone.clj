(ns com.sixsq.slipstream.auth.cyclone
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [clj-http.client :as http]
            [com.sixsq.slipstream.auth.utils.config :as cf]
            [com.sixsq.slipstream.auth.utils.http :as uh]
            [com.sixsq.slipstream.auth.utils.certs :as certs]
            [com.sixsq.slipstream.auth.sign :as sg]
            [com.sixsq.slipstream.auth.utils.external :as ex]))

(def ^:private cyclone-base-url
  "https://federation.cyclone-project.eu/auth/realms/master/protocol/openid-connect")

(def cyclone-public-key (delay (certs/read-public-key "cyclone_pubkey.pem")))

(defn- redirect_uri
  []
  (str (cf/mandatory-property-value :auth-server) "/auth/callback-cyclone"))

(defn- cyclone-code-url
  []
  (str cyclone-base-url
       (format "/auth?client_id=slipstream&redirect_uri=%s&response_type=code" (redirect_uri))))

(defn- cyclone-token-url
  []
  (str cyclone-base-url "/token"))

(defn login
  []
  (log/debug "starting CYCLONE login")
  (uh/response-redirect (cyclone-code-url)))

(defn login-name
  [claims]
  (->> claims
       ((juxt :name :preferred_username))
       (remove empty?)
       first
       ex/sanitize-login-name))

(defn callback-cyclone
  [request redirect-server]
  (try
    (let [code (uh/param-value request :code)
          token-response (http/post (cyclone-token-url)
                                    {:headers     {"Accept" "application/json"}
                                     :form-params {:grant_type   "authorization_code"
                                                   :code         code
                                                   :redirect_uri (redirect_uri)
                                                   :client_id    "slipstream"}})
          access-token (-> token-response
                           :body
                           (json/read-str :key-fn keyword)
                           :access_token)
          claims (sg/unsign-claims access-token @cyclone-public-key)]
      (log/debug "CYCLONE claims:" claims)
      (log/info "successful CYCLONE login for" (login-name claims))
      (ex/redirect-with-matched-user :cyclone (login-name claims) (:email claims) redirect-server))
    (catch Exception e
      (log/error "FAILED CYCLONE login with exception" e)
      (uh/response-redirect (str redirect-server "/dashboard")))))
