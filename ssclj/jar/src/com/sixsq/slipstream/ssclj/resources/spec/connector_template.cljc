(ns com.sixsq.slipstream.ssclj.resources.spec.connector-template
  (:require
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; Generic type definitions.

(s/def :cimi.connector-template.core/identifier
  (su/regex-string #"[a-z0-9-]" #"^[a-z0-9]+(-[a-z0-9]+)*$"))

;; Parameter definitions.

(s/def :cimi.connector-template/cloudServiceType :cimi.connector-template.core/identifier)
(s/def :cimi.connector-template/instanceName :cimi.connector-template.core/identifier)
(s/def :cimi.connector-template/orchestratorImageid string?)
(s/def :cimi.connector-template/quotaVm :cimi.core/nonblank-string) ;; FIXME: Should be nat-int? with 0 indicating no quota.
(s/def :cimi.connector-template/maxIaasWorkers pos-int?)

(s/def :cimi.connector-template/endpoint string?)
(s/def :cimi.connector-template/nativeContextualization :cimi.core/nonblank-string)
(s/def :cimi.connector-template/orchestratorSSHUsername string?)
(s/def :cimi.connector-template/orchestratorSSHPassword string?)
(s/def :cimi.connector-template/securityGroups string?)
(s/def :cimi.connector-template/updateClientURL string?)

(s/def :cimi.connector-template/href :cimi.core/resource-href) ;; FIXME: Ensure this always references the same resource type.

;;
;; Keys specifications for ConnectorTemplate resources.
;; As this is a "base class" for ConnectorTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def connector-template-keys-spec {:req-un [:cimi.connector-template/cloudServiceType
                                            :cimi.connector-template/instanceName
                                            :cimi.connector-template/orchestratorImageid
                                            :cimi.connector-template/quotaVm
                                            :cimi.connector-template/maxIaasWorkers

                                            :cimi.connector-template/endpoint
                                            :cimi.connector-template/nativeContextualization
                                            :cimi.connector-template/orchestratorSSHUsername
                                            :cimi.connector-template/orchestratorSSHPassword
                                            :cimi.connector-template/securityGroups
                                            :cimi.connector-template/updateClientURL]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs connector-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        connector-template-keys-spec
                        {:opt-un [:cimi.connector-template/href]}]))

