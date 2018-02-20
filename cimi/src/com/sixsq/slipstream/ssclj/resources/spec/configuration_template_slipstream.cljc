(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template-slipstream
    (:require
      [clojure.spec.alpha :as s]
      [com.sixsq.slipstream.ssclj.util.spec :as su]
      [com.sixsq.slipstream.ssclj.resources.spec.configuration-template :as ps]))

(s/def :cimi.configuration-template.slipstream/slipstreamVersion :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/serviceURL :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/reportsLocation :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/supportEmail :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/clientBootstrapURL :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/clientURL :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/connectorOrchPrivateSSHKey :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/connectorOrchPublicSSHKey :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/connectorLibcloudURL :cimi.core/nonblank-string)

(s/def :cimi.configuration-template.slipstream/mailUsername :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/mailPassword :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/mailHost :cimi.core/nonblank-string)
(s/def :cimi.configuration-template.slipstream/mailPort :cimi.core/port)
(s/def :cimi.configuration-template.slipstream/mailSSL boolean?)
(s/def :cimi.configuration-template.slipstream/mailDebug boolean?)

(s/def :cimi.configuration-template.slipstream/quotaEnable boolean?)

(s/def :cimi.configuration-template.slipstream/registrationEnable boolean?)
(s/def :cimi.configuration-template.slipstream/registrationEmail :cimi.core/nonblank-string)

(s/def :cimi.configuration-template.slipstream/meteringEnable boolean?)
(s/def :cimi.configuration-template.slipstream/meteringEndpoint :cimi.core/nonblank-string)

(s/def :cimi.configuration-template.slipstream/serviceCatalogEnable boolean?)

;; FIXME: used only for compatibilty with the Java server. To be removed.
(s/def :cimi.configuration-template.slipstream/cloudConnectorClass string?)

(s/def :cimi.configuration-template.slipstream/metricsLoggerEnable boolean?)
(s/def :cimi.configuration-template.slipstream/metricsGraphiteEnable boolean?)

(def configuration-template-keys-spec-req
  {:req-un [:cimi.configuration-template.slipstream/slipstreamVersion
            :cimi.configuration-template.slipstream/serviceURL
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
            :cimi.configuration-template.slipstream/metricsGraphiteEnable]
   :opt-un [:cimi.configuration-template.slipstream/reportsLocation]}) ; reportsLocation is deprecated

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
