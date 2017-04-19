(ns com.sixsq.slipstream.auth.github
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clj-http.client :as http]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.config :as cf]
    [com.sixsq.slipstream.auth.utils.external :as ex]))

(defn parse-github-user
  [user-info]
  (-> user-info
      :body
      (json/read-str :key-fn keyword)
      (select-keys [:login :email])))

(defn primary-or-verified
  "Return primary verified email (if found) else fallbacks to any (non deterministic) verified email"
  [emails]
  (let [verified (filter :verified emails)]
    (if-let [primary (first (filter :primary verified))]
      (:email primary)
      (:email (first verified)))))

(defn- retrieve-private-email
  [access-token]
  (let [user-emails-response (http/get "https://api.github.com/user/emails"
                                       {:headers {"Authorization" (str "token " access-token)}})
        user-emails          (-> user-emails-response :body (json/read-str :key-fn keyword))]
    (primary-or-verified user-emails)))

(defn- retrieve-email
  [user-info access-token]
  (if-let [public-email (:email user-info)]
    public-email
    (retrieve-private-email access-token)))

(defn- sanitized-login
  [user-info]
  (-> user-info :login ex/sanitize-login-name))

(defn callback-github
  [request redirect-server]
  (try
    (let [oauth-code            (uh/param-value request :code)
          access-token-response (http/post "https://github.com/login/oauth/access_token"
                                           {:headers     {"Accept" "application/json"}
                                            :form-params {:client_id     (cf/mandatory-property-value :github-client-id)
                                                          :client_secret (cf/mandatory-property-value :github-client-secret)
                                                          :code          oauth-code}})
          access-token          (-> access-token-response
                                    :body
                                    (json/read-str :key-fn keyword)
                                    :access_token)

          user-info-response    (http/get "https://api.github.com/user"
                                          {:headers {"Authorization" (str "token " access-token)}})

          user-info             (parse-github-user user-info-response)]

      (log/debug "Github user-info " user-info)
      (log/info "successful GitHub login for" (sanitized-login user-info))
      (ex/redirect-with-matched-user :github
                                     (sanitized-login user-info)
                                     (retrieve-email user-info access-token)
                                     redirect-server))

    (catch Exception e
      (log/error "FAILED GitHub login with exception" e)
      (uh/response-redirect (str redirect-server "/dashboard")))))

(defn login
  []
  (log/debug "starting GitHub login")
  (uh/response-redirect
    (format
      "https://github.com/login/oauth/authorize?client_id=%s&scope=user:email"
      (cf/mandatory-property-value :github-client-id))))

