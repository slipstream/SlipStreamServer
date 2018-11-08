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

(def ^:const credential-href-regex #"^credential/[a-z0-9]+(-[a-z0-9]+)*(_\d+)?$")
(s/def ::href (s/and string? #(re-matches credential-href-regex %)))
(s/def ::secret string?)
(s/def ::clientAPIKey (su/only-keys :req-un [::href
                                             ::secret]))

(s/def ::sshPublicKeys string?)

(s/def ::deploymentTemplate ::cimi-common/resource-link)

(def deployment-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        deployment-template/deployment-template-keys-spec
                        {:req-un [::state
                                  ::clientAPIKey]
                         :opt-un [::deploymentTemplate
                                  ::sshPublicKeys]}]))

(s/def ::deployment (su/only-keys-maps deployment-keys-spec))
