(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-slipstream
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]))

(s/def :cimi.configuration-template.slipstream/slipstreamVersion :cimi.core/nonblank-string) ;; 3.12-SNAPSHOT
(s/def :cimi.configuration-template.slipstream/serviceURL :cimi.core/nonblank-string) ;; https://nuv.la
(s/def :cimi.configuration-template.slipstream/reportsLocation :cimi.core/nonblank-string) ;; /var/tmp/slipstream/reports
(s/def :cimi.configuration-template.slipstream/supportEmail :cimi.core/nonblank-string) ;; support@example.com
(s/def :cimi.configuration-template.slipstream/clientBootstrapURL :cimi.core/nonblank-string) ;; https://185.19.28.68/downloads/slipstream.bootstrap
(s/def :cimi.configuration-template.slipstream/clientURL :cimi.core/nonblank-string) ;; https://185.19.28.68/downloads/slipstreamclient.tgz
(s/def :cimi.configuration-template.slipstream/connectorOrchPrivateSSHKey :cimi.core/nonblank-string) ;; /opt/slipstream/server/.ssh/id_rsa
(s/def :cimi.configuration-template.slipstream/connectorOrchPublicSSHKey :cimi.core/nonblank-string) ;; /opt/slipstream/server/.ssh/id_rsa.pub
(s/def :cimi.configuration-template.slipstream/connectorLibcloudURL :cimi.core/nonblank-string) ;; https://185.19.28.68/downloads/libcloud.tgz

(s/def :cimi.configuration-template.slipstream/mailUsername :cimi.core/nonblank-string) ;; mailer@example.com
(s/def :cimi.configuration-template.slipstream/mailPassword :cimi.core/nonblank-string) ;; plain-text
(s/def :cimi.configuration-template.slipstream/mailHost :cimi.core/nonblank-string) ;; smtp.example.com
(s/def :cimi.configuration-template.slipstream/mailPort :cimi.core/port) ;; 465
(s/def :cimi.configuration-template.slipstream/mailSSL boolean?) ;; true
(s/def :cimi.configuration-template.slipstream/mailDebug boolean?) ;; true

(s/def :cimi.configuration-template.slipstream/quotaEnable boolean?) ;; true

(s/def :cimi.configuration-template.slipstream/registrationEnable boolean?) ;; true
(s/def :cimi.configuration-template.slipstream/registrationEmail :cimi.core/nonblank-string) ;; true

(s/def :cimi.configuration-template.slipstream/meteringEnable boolean?) ;; false
(s/def :cimi.configuration-template.slipstream/meteringEndpoint :cimi.core/nonblank-string) ;; http://localhost:2005

(s/def :cimi.configuration-template.slipstream/serviceCatalogEnable boolean?) ;; true

;; FIXME: used only for compatibilty with the Java server. To be removed.
(s/def :cimi.configuration-template.slipstream/cloudConnectorClass string?) ;; "name-region-az:connector,"

(s/def :cimi.configuration-template.slipstream/metricsLoggerEnable boolean?) ;; false
(s/def :cimi.configuration-template.slipstream/metricsGraphiteEnable boolean?) ;; false

(def configuration-template-keys-spec-req
  {:req-un [:cimi.configuration-template.slipstream/slipstreamVersion
            :cimi.configuration-template.slipstream/serviceURL
            :cimi.configuration-template.slipstream/reportsLocation
            :cimi.configuration-template.slipstream/supportEmail
            :cimi.configuration-template.slipstream/clientBootstrapURL
            :cimi.configuration-template.slipstream/clientURL
            :cimi.configuration-template.slipstream/connectorOrchPrivateSSHKey
            :cimi.configuration-template.slipstream/connectorOrchPublicSSHKey
            :cimi.configuration-template.slipstream/connectorLibcloudURL

            :cimi.configuration-template.slipstream/mailUsername
            :cimi.configuration-template.slipstream/mailPassword
            :cimi.configuration-template.slipstream/mailHost
            :cimi.configuration-template.slipstream/mailPort
            :cimi.configuration-template.slipstream/mailSSL
            :cimi.configuration-template.slipstream/mailDebug

            :cimi.configuration-template.slipstream/quotaEnable

            :cimi.configuration-template.slipstream/registrationEnable
            :cimi.configuration-template.slipstream/registrationEmail

            :cimi.configuration-template.slipstream/meteringEnable
            :cimi.configuration-template.slipstream/meteringEndpoint

            :cimi.configuration-template.slipstream/serviceCatalogEnable

            :cimi.configuration-template.slipstream/cloudConnectorClass

            :cimi.configuration-template.slipstream/metricsLoggerEnable
            :cimi.configuration-template.slipstream/metricsGraphiteEnable]})

;; FIXME: Treats all parameters as optional.  Instead those without reasonable defaults should be required.
(def configuration-template-keys-spec-opt
  {:opt-un (:req-un configuration-template-keys-spec-req)})

;; Defines the contents of the slipstream ConfigurationTemplate resource itself.
(s/def :cimi/configuration-template.slipstream
  (su/only-keys-maps ps/resource-keys-spec
                     configuration-template-keys-spec-req))

;; Defines the contents of the slipstream template used in a create resource.
;; NOTE: The name must match the key defined by the resource, :configurationTemplate here.
(s/def :cimi.configuration-template.slipstream/configurationTemplate
  (su/only-keys-maps ps/template-keys-spec
                     configuration-template-keys-spec-opt))

(s/def :cimi/configuration-template.slipstream-create
  (su/only-keys-maps ps/create-keys-spec
                     {:opt-un [:cimi.configuration-template.slipstream/configurationTemplate]}))
