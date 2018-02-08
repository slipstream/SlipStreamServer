(ns com.sixsq.slipstream.ssclj.resources.spec.connector-template
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; Generic type definitions.

(s/def :cimi.connector-template.core/identifier
  (su/regex-string #"[a-z0-9-]" #"^[a-z0-9]+(-[a-z0-9]+)*$"))

;; Required parameters for all connectors.
(s/def :cimi.connector-template/cloudServiceType :cimi.connector-template.core/identifier)
(s/def :cimi.connector-template/instanceName :cimi.connector-template.core/identifier)
(s/def :cimi.connector-template/orchestratorImageid string?)
(s/def :cimi.connector-template/quotaVm string?)            ;; FIXME: Should be nat-int? with 0 indicating no quota.
(s/def :cimi.connector-template/maxIaasWorkers pos-int?)

;; Common parameters, but which are not used by all.
;; Add these to the connector schema as necessary.
(s/def :cimi.connector-template/endpoint string?)
(s/def :cimi.connector-template/nativeContextualization :cimi.core/nonblank-string)
(s/def :cimi.connector-template/orchestratorSSHUsername string?)
(s/def :cimi.connector-template/orchestratorSSHPassword string?)
(s/def :cimi.connector-template/securityGroups string?)
(s/def :cimi.connector-template/updateClientURL string?)

(def connector-template-regex #"^connector-template/[a-z0-9]+(-[a-z0-9]+)*$")
(s/def :cimi.connector-template/href (s/and string? #(re-matches connector-template-regex %)))


(s/def :cimi.connector-template/connectorTemplate (su/only-keys-maps
                                                    {:req-un [:cimi.connector-template/href]}))
;;
;; Keys specifications for ConnectorTemplate resources.
;; As this is a "base class" for ConnectorTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def connector-template-keys-spec {:req-un [:cimi.connector-template/cloudServiceType
                                            :cimi.connector-template/instanceName
                                            :cimi.connector-template/orchestratorImageid
                                            :cimi.connector-template/quotaVm
                                            :cimi.connector-template/maxIaasWorkers]
                                   :opt-un [:cimi.connector-template/connectorTemplate]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs connector-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        connector-template-keys-spec
                        {:opt-un [:cimi.connector-template/href]}]))

