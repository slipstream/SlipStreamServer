(ns com.sixsq.slipstream.ssclj.resources.spec.deployment
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template :as deployment-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::state #{"CREATED",
                 "STARTING", "STARTED",
                 "STOPPING", "STOPPED",
                 "PAUSING", "PAUSED",
                 "SUSPENDING", "SUSPENDED",
                 "ERROR"})

(s/def ::module ::cimi-common/resource-link)

(s/def ::deploymentTemplate ::cimi-common/resource-link)

(def deployment-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        {:req-un [::state
                                  ::module]
                         :opt-un [::deploymentTemplate]}]))

(s/def ::deployment (su/only-keys-maps deployment-keys-spec))
