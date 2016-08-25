(ns com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream
  (:require
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const service "slipstream")

;;
;; schemas
;;

(def ConfigurationTemplateAttrs
  (merge p/ConfigurationTemplateAttrs
         {:serviceURL                 c/NonBlankString      ;; https://nuv.la
          :reportsLocation            c/NonBlankString      ;; /var/tmp/slipstream/reports
          :supportEmail               c/NonBlankString      ;; support@example.com
          :clientBootstrapURL         c/NonBlankString      ;; https://185.19.28.68/downloads/slipstream.bootstrap
          :clientURL                  c/NonBlankString      ;; https://185.19.28.68/downloads/slipstreamclient.tgz
          :connectorOrchPrivateSSHKey c/NonBlankString      ;; /opt/slipstream/server/.ssh/id_rsa
          :connectorOrchPublicSSHKey  c/NonBlankString      ;; /opt/slipstream/server/.ssh/id_rsa.pub
          :connectorLibcloudURL       c/NonBlankString      ;; https://185.19.28.68/downloads/libcloud.tgz

          :mailUsername               c/NonBlankString      ;; mailer@example.com
          :mailPassword               c/NonBlankString      ;; plain-text
          :mailHost                   c/NonBlankString      ;; smtp.example.com
          :mailPort                   c/PosInt              ;; 465
          :mailSSL                    s/Bool                ;; true
          :mailDebug                  s/Bool                ;; true

          :quotaEnable                s/Bool                ;; true

          :registrationEnable         s/Bool                ;; true
          :registrationEmail          c/NonBlankString      ;; register@sixsq.com

          :prsEnable                  s/Bool                ;; true
          :prsEndpoint                c/NonBlankString      ;; http://localhost:8203/filter-rank

          :meteringEnable             s/Bool                ;; false
          :meteringEndpoint           c/NonBlankString      ;; http://localhost:2005

          :serviceCatalogEnable       s/Bool                ;; true
          }))

(def ConfigurationTemplate
  (merge p/ConfigurationTemplate
         ConfigurationTemplateAttrs))

(def ConfigurationTemplateRef
  (s/constrained
    (merge ConfigurationTemplateAttrs
           {(s/optional-key :href) c/NonBlankString})
    seq 'not-empty?))

;;
;; resource
;;
(def ^:const resource
  {:service                    service
   :name                       "SlipStream"
   :description                "SlipStream Service Configuration"
   :serviceURL                 "https://localhost"
   :reportsLocation            "/var/tmp/slipstream/reports"
   :supportEmail               "support@example.com"
   :clientBootstrapURL         "https://localhost/downloads/slipstream.bootstrap"
   :clientURL                  "https://localhost/downloads/slipstreamclient.tgz"
   :connectorOrchPrivateSSHKey "/opt/slipstream/server/.ssh/id_rsa"
   :connectorOrchPublicSSHKey  "/opt/slipstream/server/.ssh/id_rsa.pub"
   :connectorLibcloudURL       "https://localhost/downloads/libcloud.tgz"

   :mailUsername               "mailer"
   :mailPassword               "change-me"
   :mailHost                   "smtp.example.com"
   :mailPort                   465
   :mailSSL                    true
   :mailDebug                  true

   :quotaEnable                true

   :registrationEnable         true
   :registrationEmail          "register@sixsq.com"

   :prsEnable                  true
   :prsEndpoint                "http://localhost:8203/filter-rank"

   :meteringEnable             false
   :meteringEndpoint           "http://localhost:2005"

   :serviceCatalogEnable       false
   })

;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         {:alphaKey {:displayName "Alpha Key"
                     :category    "general"
                     :description "example parameter"
                     :type        "int"
                     :mandatory   true
                     :readOnly    false
                     :order       1}}))

;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn ConfigurationTemplate))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
