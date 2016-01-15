(ns com.sixsq.slipstream.auth.cyclone
  (:require [clojure.tools.logging :as log]
            [com.sixsq.slipstream.auth.utils.http :as uh]
            [com.sixsq.slipstream.auth.utils.config :as cf]
            [clj-http.client :as http]
            [clojure.data.json :as json]
            [com.sixsq.slipstream.auth.sign :as sg]))

(def ^:private cyclone-base-url
  "https://federation.cyclone-project.eu/auth/realms/master/protocol/openid-connect")

(def ^:private redirect_uri "http://localhost:8201/auth/callback-cyclone")

(def ^:private cyclone-code-url
  (str cyclone-base-url
       (format "/auth?client_id=slipstream&redirect_uri=%s&response_type=code" redirect_uri)))

(def ^:private cyclone-token-url
  (str cyclone-base-url "/token"))

(defn login
  [_]
  (log/info "Cyclone authentication.")
  (uh/response-redirect cyclone-code-url))

(defn- callback-cyclone-code
  [code redirect-server]
  (log/info "Callback Cyclone CODE request for code " code)
  (let [token-response  (http/post cyclone-token-url
                                      {:headers     {"Accept"       "application/json"}
                                       :form-params {:grant_type    "authorization_code"
                                                     :code          code
                                                     :redirect_uri  redirect_uri
                                                     :client_id     "slipstream"}})
        access-token    (-> token-response
                            :body
                            (json/read-str :key-fn keyword)
                            :access_token)
        claims          (sg/unsign-claims access-token "cyclone_pubkey.pem")]
    (log/info "access token = " access-token)
    (log/info "claims = " claims)
    (uh/response-redirect (str redirect-server "/dashboard"))))

(defn callback-cyclone-token
  []
  (log/info "callback-cyclone-token"))

(defn callback-cyclone
  [request redirect-server]

  (log/info "Callback Cyclone")

  (if-let [code (uh/param-value request :code)]
    (callback-cyclone-code code redirect-server)
    (callback-cyclone-token)))