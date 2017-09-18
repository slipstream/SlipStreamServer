(ns com.sixsq.slipstream.ssclj.resources.deployment-std
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.deployment]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template-std]
    [com.sixsq.slipstream.ssclj.resources.deployment.java-to-clj-deployment :as java-to-clj-deployment]
    [com.sixsq.slipstream.ssclj.resources.deployment :as d]
    [com.sixsq.slipstream.ssclj.resources.deployment-template-std :as dtpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [environ.core :as env]
    [clj-http.client :as http]
    [clojure.data.json :as json]))

(def slipstream-java-endpoint (or (env/env :slipstream-java-endpoint) "http://localhost:8182"))

;;
;; validate the create resource
;;
(def create-validate-fn (u/create-spec-validation-fn :cimi/deployment-template.std-create))
(defmethod d/create-validate-subtype dtpl/method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into deployment resource
;; just strips method attribute and updates the resource URI
;;
(defmethod d/tpl->deployment dtpl/method
  [{module :module :as resource} {username :user-name :as request}]
  (let [java-deployment-location (-> (http/post (str slipstream-java-endpoint "/run")
                                                {:headers     {"slipstream-authn-info" username}
                                                 :form-params {:refqname module :bypass-ssh-check true}})
                                     (get-in [:headers "Location"]))
        java-deployment-json (-> (http/get java-deployment-location {:headers {"slipstream-authn-info" username
                                                                               "Accept" "application/json"}})
                                 :body
                                 (json/read-str :key-fn keyword))
        ]
    (java-to-clj-deployment/transform java-deployment-json)))

