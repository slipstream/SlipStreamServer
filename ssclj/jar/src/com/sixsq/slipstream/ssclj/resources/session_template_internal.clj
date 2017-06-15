(ns com.sixsq.slipstream.ssclj.resources.session-template-internal
  (:require
    [clojure.spec.alpha :as s]
    [clojure.stacktrace :as st]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-internal]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.db.impl :as db]))

(def ^:const authn-method "internal")

(def default-template {:method      authn-method
                       :instance    authn-method
                       :name        "Internal"
                       :description "Internal Authentication via Username/Password"
                       :username    "username"
                       :password    "password"
                       :acl         p/resource-acl})

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription
         {:username {:displayName "Username"
                     :category    "general"
                     :description "username"
                     :type        "string"
                     :mandatory   true
                     :readOnly    false
                     :order       3}
          :password {:displayName "Password"
                     :category    "general"
                     :description "password"
                     :type        "password"
                     :mandatory   true
                     :readOnly    false
                     :order       4}}))

;;
;; initialization: register this Session template and create internal authentication template
;;
(defn initialize
  []
  (p/register authn-method desc)
  (try
    (let [request {:params   {:resource-name p/resource-url}
                   :identity {:current         "INTERNAL"
                              :authentications {"INTERNAL" {:identity "INTERNAL", :roles ["ADMIN" "USER" "ANON"]}}}
                   :body     default-template}
          {:keys [status]} (crud/add request)]
      (case status
        201 (log/info "created session-template/internal resource")
        409 (log/info "session-template/internal resource already exists; new resource not created.")
        (log/info "unexpected status code when creating session-template/internal resource:" status)))
    (catch Exception e
      (log/warn "error when creating session-template/internal resource: " (str e) "\n"
                (with-out-str (st/print-cause-trace e))))))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.internal))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
