(ns com.sixsq.slipstream.auth.oidc
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]))


(defn get-access-token
  [client-id client-secret base-url tokenURL oidc-code redirect-uri]
  (-> (http/post (or tokenURL (str base-url "/token"))
                 {:headers     {"Accept" "application/json"}
                  :form-params {:grant_type    "authorization_code"
                                :code          oidc-code
                                :redirect_uri  redirect-uri
                                :client_id     client-id
                                :client_secret client-secret}})
      :body
      (json/read-str :key-fn keyword)
      :access_token))
