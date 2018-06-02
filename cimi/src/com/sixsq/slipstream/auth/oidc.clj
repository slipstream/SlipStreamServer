(ns com.sixsq.slipstream.auth.oidc
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]))


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
