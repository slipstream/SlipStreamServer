(ns com.sixsq.slipstream.ssclj.resources.configuration-template-slipstream.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]
    [com.sixsq.slipstream.ssclj.resources.configuration-template.spec :as ct]))

(s/def ::serviceURL ::c/nonblank-string)                    ;; "https://localhost"
(s/def ::resportsLocation ::c/nonblank-string)              ;; "/var/tmp/slipstream/reports"
(s/def ::supportEmail ::c/nonblank-string)                  ;; "support@example.com"
(s/def ::clientBootstrapURL ::c/nonblank-string)            ;; "https://localhost/downloads/slipstream.bootstrap"
(s/def ::clientURL ::c/nonblank-string)                     ;; "https://localhost/downloads/slipstreamclient.tgz"

(s/def ::connectorOrchPrivateSSHKey ::c/nonblank-string)    ;; "/opt/slipstream/server/.ssh/id_rsa"
(s/def ::connectorOrchPublicSSHKey ::c/nonblank-string)     ;; "/opt/slipstream/server/.ssh/id_rsa.pub"
(s/def ::connectorLibcloudURL ::c/nonblank-string)          ;; "https://localhost/downloads/libcloud.tgz"

(s/def ::mailUsername ::c/nonblank-string)                  ;; "mailer"
(s/def ::mailPassword ::c/nonblank-string)                  ;; "change-me"
(s/def ::mailHost ::c/nonblank-string)                      ;; "smtp.example.com"
(s/def ::mailPort ::c/port)                                 ;; 465
(s/def ::mailSSL boolean?)                                  ;; true
(s/def ::mailDebug boolean?)                                ;; true

(s/def ::quotaEnable boolean?)                              ;; true

(s/def ::registrationEnable boolean?)                       ;; true
(s/def ::registrationEmail ::c/nonblank-string)             ;; "register@sixsq.com"

(s/def ::prsEnable boolean?)                                ;; true
(s/def ::prsEndpoint ::c/nonblank-string)                   ;; "http://localhost:8203/filter-rank"

(s/def ::meteringEnable boolean?)                           ;; false
(s/def ::meteringEndpoint ::c/nonblank-string)              ;; "http://localhost:2005"

(s/def ::serviceCatalogEnable boolean?)                     ;; false

(s/def ::slipstreamVersion ::c/nonblank-string)             ;; slipstream-version

(s/def ::cloudConnectorClass string?)                       ;; ""


(defmethod ct/service-type "slipstream"
  [_]
  (c/only-keys :req-un [::c/id
                        ::c/resourceURI
                        ::c/acl

                        ::ct/service

                        ::serviceURL
                        ::reportsLocation
                        ::supportEmail
                        ::clientBootstrapURL
                        ::clientURL

                        ::connectorOrchPrivateSSHKey
                        ::connectorOrchPublicSSHKey
                        ::connectorLibcloudURL

                        ::mailUsername
                        ::mailPassword
                        ::mailHost
                        ::mailPort
                        ::mailSSL
                        ::mailDebug

                        ::quotaEnable

                        ::registrationEnable
                        ::registrationEmail

                        ::prsEnable
                        ::prsEndpoint

                        ::meteringEnable
                        ::meteringEndpoint

                        ::serviceCatalogEnable

                        ::slipstreamVersion

                        ::cloudConnectorClass]
               :opt-un [::c/created            ;; FIXME: should be required
                        ::c/updated            ;; FIXME: should be required
                        ::c/name
                        ::c/description
                        ::c/properties
                        ::c/operations]))

