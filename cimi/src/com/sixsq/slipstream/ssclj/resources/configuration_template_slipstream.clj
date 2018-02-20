(ns com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.config :as uc]))

(def ^:const service "slipstream")

(def slipstream-version (-> "com/sixsq/slipstream/version.txt"
                            uc/find-file
                            slurp))

;;
;; resource
;;
(def ^:const resource
  {
   :service                    service
   :name                       "SlipStream"
   :description                "SlipStream Service Configuration"
   :serviceURL                 "https://localhost"
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

   :meteringEnable             false
   :meteringEndpoint           "http://localhost:2005"

   :serviceCatalogEnable       false

   :slipstreamVersion          slipstream-version

   :cloudConnectorClass        ""

   :metricsLoggerEnable        false
   :metricsGraphiteEnable      false})


;;
;; description
;;
(def ^:const desc
  (merge p/ConfigurationTemplateDescription
         (uc/read-config "com/sixsq/slipstream/ssclj/resources/configuration-slipstream-desc.edn")))

;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/configuration-template.slipstream))
(defmethod p/validate-subtype service
  [resource]
  (validate-fn resource))
