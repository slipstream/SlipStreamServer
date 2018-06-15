(ns com.sixsq.slipstream.auth.oidc
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.string :as str])
  (:import java.util.Base64)
  )


(defn get-oidc-access-token
  [oidc-client-id oidc-client-secret oidc-base-url oidc-token-url oidc-code redirect-uri]
  (-> (http/post (or oidc-token-url (str oidc-base-url "/token"))
                 {:headers     {"Accept" "application/json"}
                  :form-params {:grant_type    "authorization_code"
                                :code          oidc-code
                                :redirect_uri  redirect-uri
                                :client_id     oidc-client-id
                                :client_secret oidc-client-secret}})
      :body
      (json/read-str :key-fn keyword)
      :access_token))


