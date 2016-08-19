(ns com.sixsq.slipstream.ssclj.resources.configuration
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]))

(def ^:const resource-tag :configurations)

(def ^:const resource-name "Configuration")

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ConfigurationCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}})
;;
;; schemas
;;

(def Configuration
  (merge c/CommonAttrs
         c/AclAttr
         {:serviceURL                 c/NonBlankString      ;; https://nuv.la
          :reportsLocation            c/NonBlankString      ;; /var/tmp/slipstream/reports
          :supportEmail               c/NonBlankString      ;; support@example.com
          :clientBootstrapURL         c/NonBlankString      ;; https://185.19.28.68/downloads/slipstream.bootstrap
          :clientURL                  c/NonBlankString      ;; https://185.19.28.68/downloads/slipstreamclient.tgz
          :connectorOrchPrivateSSHKey c/NonBlankString      ;; /opt/slipstream/server/.ssh/id_rsa
          :connectorOrchPublicSSHKey  c/NonBlankString      ;; /opt/slipstream/server/.ssh/id_rsa.pub
          :connectorLibcloudURL       c/NonBlankString      ;; https://185.19.28.68/downloads/libcloud.tgz

          :mail-username              c/NonBlankString      ;; mailer@example.com
          :mail-password              c/NonBlankString      ;; plain-text
          :mail-host                  c/NonBlankString      ;; smtp.example.com
          :mail-port                  c/PosInt              ;; 465
          :mail-ssl                   s/Bool                ;; true
          :mail-debug                 s/Bool                ;; true

          :quota-enable               s/Bool                ;; true

          :registration-enable        s/Bool                ;; true

          :prs-enable                 s/Bool                ;; true
          :prs-endpoint               c/NonBlankString      ;; http://localhost:8203/filter-rank

          :metering-enable            s/Bool                ;; false
          :metering-endpoint          c/NonBlankString      ;; http://localhost:2005

          :service-catalog-enable     s/Bool                ;; true
          }))

(def default-configuration {:name                       "SlipStream"
                            :description                "SlipStream Service Configuration"
                            :serviceURL                 "https://localhost"
                            :reportsLocation            "/var/tmp/slipstream/reports"
                            :supportEmail               "support@example.com"
                            :clientBootstrapURL         "https://localhost/downloads/slipstream.bootstrap"
                            :clientURL                  "https://localhost/downloads/slipstreamclient.tgz"
                            :connectorOrchPrivateSSHKey "/opt/slipstream/server/.ssh/id_rsa"
                            :connectorOrchPublicSSHKey  "/opt/slipstream/server/.ssh/id_rsa.pub"
                            :connectorLibcloudURL       "https://localhost/downloads/libcloud.tgz"

                            :mail-username              "mailer"
                            :mail-password              "change-me"
                            :mail-host                  "smtp.example.com"
                            :mail-port                  465
                            :mail-ssl                   true
                            :mail-debug                 true

                            :quota-enable               true

                            :registration-enable        true

                            :prs-enable                 true
                            :prs-endpoint               "http://localhost:8203/filter-rank"

                            :metering-enable            false
                            :metering-endpoint          "http://localhost:2005"

                            :service-catalog-enable     false
                            })

;;
;; always provide the same identifier
;;
(defmethod crud/new-identifier resource-name
  [json resource-name]
  (let [new-id (str resource-url "/slipstream" )]
    (assoc json :id new-id)))

;;
;; multimethods for validation and operations
;;

(def validate-fn (u/create-validation-fn Configuration))
(defmethod crud/validate resource-uri
  [resource]
  (validate-fn resource))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))
(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))
(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))
(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))
(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))
(defmethod crud/query resource-name
  [request]
  (query-impl request))

;;
;; initialization: create the service configuration, if necessary
;;
#_(defn initialize
    []
    (try
      (add default-configuration)
      (log/info "Created" resource-name "resource")
      (catch Exception e
        (log/warn resource-name "resource not created; may already exist; message: " (str e)))))

