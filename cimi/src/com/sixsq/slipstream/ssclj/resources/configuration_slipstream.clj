(ns com.sixsq.slipstream.ssclj.resources.configuration-slipstream
  (:require
    [clojure.tools.logging :as log]

    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template-slipstream]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.configuration :as p]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream :as tpl]))

(def ^:const service "slipstream")

(def ^:const instance-url (str p/resource-url "/" service))

(def ConfigurationDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc ConfigurationDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.slipstream))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/configuration-template.slipstream-create))
(defmethod p/create-validate-subtype service
  [resource]
  (create-validate-fn resource))

(def user-roles "root ADMIN")

(defn complete-resource
  "Completes the given document with server-managed information:
   resourceURI, timestamps, operations, and ACL."
  [{:keys [service] :as resource}]
  (when service
    (let [href (str instance-url "/describe")
          ops [{:rel (:describe c/action-uri) :href href}]]
      (-> resource
          (merge {:id          instance-url
                  :resourceURI ct/resource-uri
                  :acl         ct/resource-acl
                  :operations  ops})
          u/update-timestamps))))

(defn as-request
  [body resource-uuid user-roles-str]
  (let [request {:params  {:uuid resource-uuid}
                 :body    (or body {})
                 :headers {aih/authn-info-header user-roles-str}}]
    ((aih/wrap-authn-info-header identity) request)))


(defn add
  []
  (-> tpl/resource
      complete-resource
      (as-request service user-roles)
      p/add-impl))

;; initialization: create initial service configuration if necessary
;;
(defn initialize
  []
  (try
    (add)
    (log/info (format "Created %s record" instance-url))
    (catch Exception e
      (log/warn instance-url "resource not created; may already exist; message: " (str e)))))
