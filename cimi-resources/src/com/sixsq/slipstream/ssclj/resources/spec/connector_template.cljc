(ns com.sixsq.slipstream.ssclj.resources.spec.connector-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;; Generic type definitions.

(s/def ::identifier (s/and string? #(re-matches #"^[a-z0-9]+(-[a-z0-9]+)*$" %)))

;; Required parameters for all connectors.
(s/def ::cloudServiceType ::identifier)
(s/def ::instanceName ::identifier)
(s/def ::orchestratorImageid string?)
(s/def ::quotaVm string?)                                   ;; FIXME: Should be nat-int? with 0 indicating no quota.
(s/def ::maxIaasWorkers pos-int?)

;; Common parameters, but which are not used by all.
;; Add these to the connector schema as necessary.
(s/def ::endpoint string?)
(s/def ::objectStoreEndpoint string?)
(s/def ::nativeContextualization ::cimi-core/nonblank-string)
(s/def ::orchestratorSSHUsername string?)
(s/def ::orchestratorSSHPassword string?)
(s/def ::securityGroups string?)
(s/def ::updateClientURL string?)

(def connector-template-regex #"^connector-template/[a-z0-9]+(-[a-z0-9]+)*$")
(s/def ::href (s/and string? #(re-matches connector-template-regex %)))


(s/def ::connectorTemplate (su/only-keys :req-un [::href]))

;;
;; Keys specifications for ConnectorTemplate resources.
;; As this is a "base class" for ConnectorTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def connector-template-keys-spec {:req-un [::cloudServiceType
                                            ::instanceName
                                            ::orchestratorImageid
                                            ::quotaVm
                                            ::maxIaasWorkers]
                                   :opt-un [::connectorTemplate]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs connector-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        connector-template-keys-spec
                        {:opt-un [::href]}]))

