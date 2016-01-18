(ns com.sixsq.slipstream.auth.github
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [clj-http.client :as http]
    [com.sixsq.slipstream.auth.utils.http :as uh]
    [com.sixsq.slipstream.auth.utils.config :as cf]
    [com.sixsq.slipstream.auth.external :as ex]))

(defn parse-github-user
  [user-info]
  (-> user-info
      :body
      (json/read-str :key-fn keyword)
      (select-keys [:login :email])))

(defn callback-github
  [request redirect-server]
  (try
    (let [oauth-code (uh/param-value request :code)
          access-token-response (http/post "https://github.com/login/oauth/access_token"
                                           {:headers     {"Accept" "application/json"}
                                            :form-params {:client_id     (cf/mandatory-property-value :github-client-id)
                                                          :client_secret (cf/mandatory-property-value :github-client-secret)
                                                          :code          oauth-code}})
          access-token (-> access-token-response
                           :body
                           (json/read-str :key-fn keyword)
                           :access_token)
          user-info-response (http/get "https://api.github.com/user"
                                       {:headers {"Authorization" (str "token " access-token)}})
          user-info (parse-github-user user-info-response)]

      (ex/redirect-with-matched-user :github (:login user-info) (:email user-info) redirect-server))
    (catch Exception e
      (log/error "Invalid Github authentication " e)
      (uh/response-redirect (str redirect-server "/dashboard")))))

(defn login
  []
  (log/info "Github authentication.")
  (uh/response-redirect
    (format
      "https://github.com/login/oauth/authorize?client_id=%s&scope=user:email"
      (cf/mandatory-property-value :github-client-id))))

