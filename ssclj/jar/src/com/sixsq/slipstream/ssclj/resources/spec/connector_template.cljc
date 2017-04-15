(ns com.sixsq.slipstream.ssclj.resources.spec.connector-template
  (:require
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.connector-template/orchestratorImageid :cimi.core/nonblank-string)
(s/def :cimi.connector-template/quotaVm :cimi.core/nonblank-string)
(s/def :cimi.connector-template/maxIaasWorkers pos-int?)

(s/def :cimi.connector-template/cloudServiceType :cimi.core/nonblank-string)

(s/def :cimi.connector-template/instanceName
  (su/regex-string #"[a-z0-9-]" #"^[a-z0-9]+(-[a-z0-9-]+)*$"))

(s/def :cimi.connector-template/href :cimi.core/resource-href)

(def connector-template-attrs-keys
  (su/merge-keys-specs [c/common-attrs
                        {:req-un [:cimi.connector-template/cloudServiceType
                                  :cimi.connector-template/instanceName
                                  :cimi.connector-template/orchestratorImageid
                                  :cimi.connector-template/quotaVm
                                  :cimi.connector-template/maxIaasWorkers]}]))

(s/def :cimi/connector-template (su/only-keys-maps c/common-attrs
                                                   connector-template-attrs-keys))

(s/def :cimi/connector-template-ref (su/only-keys-maps connector-template-attrs-keys
                                                       {:opt-un [:cimi.connector-template/href]}))
