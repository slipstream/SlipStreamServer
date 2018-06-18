(ns com.sixsq.slipstream.auth.mitreid
  (:require
    [clj-http.client :as http]
    [clojure.data.json :as json]
    [clojure.string :as str]))


(defn get-mitreid-access-token
  [mitreid-client-id mitreid-client-secret mitreid-base-url mitreid-token-url mitreid-code redirect-uri]
  (-> (http/post (or mitreid-token-url (str mitreid-base-url "/token"))
                 {:headers     {"Accept" "application/json"}
                  :form-params {:grant_type    "authorization_code"
                                :code          mitreid-code
                                :redirect_uri  redirect-uri
                                :client_id     mitreid-client-id
                                :client_secret mitreid-client-secret}})
      :body
      (json/read-str :key-fn keyword)
      :access_token))


