(ns com.sixsq.slipstream.ssclj.resources.spec.deployment
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.deployment/id :cimi.core/nonblank-string)      ;TODO enhance
(s/def :cimi.deployment/module-resource-uri :cimi.core/uri)
(s/def :cimi.deployment/type #{"Run" "Orchestration" "Machine"}) ;TODO rename
(s/def :cimi.deployment/category #{"Image" "Deployment"})
(s/def :cimi.deployment/start-time :cimi.core/timestamp)
(s/def :cimi.deployment/end-time :cimi.core/timestamp)
(s/def :cimi.deployment/last-state-change-time :cimi.core/timestamp)
(s/def :cimi.deployment/mutable boolean?)
(s/def :cimi.deployment/state
  #{"init" "provisioning" "executing" "sending report" "finalyzing" "terminated" "canceled" "done"})

(s/def :cimi.deployment.parameter/description string?)
(s/def :cimi.deployment.parameter/default-value string?)
(s/def :cimi.deployment.parameter/user-choice-value string?)

(s/def :cimi.deployment/parameter (su/only-keys-maps {:opt-un [:cimi.deployment.parameter/description
                                                               :cimi.deployment.parameter/default-value
                                                               :cimi.deployment.parameter/user-choice-value]}))

(s/def :cimi.deployment.node/cpu.nb :cimi.deployment/parameter)
(s/def :cimi.deployment.node/ram.GB :cimi.deployment/parameter)
(s/def :cimi.deployment.node/disk.GB :cimi.deployment/parameter)
(s/def :cimi.deployment.node/multiplicity :cimi.deployment/parameter)
(s/def :cimi.deployment.node/max-provisioning-failures :cimi.deployment/parameter)
(s/def :cimi.deployment.node/cloudservice :cimi.deployment/parameter)

(s/def :cimi.deployment/parameters
  (su/only-keys-maps
    {:req-un [:cimi.deployment.node/cloudservice]
     :opt-un [:cimi.deployment.node/multiplicity
              :cimi.deployment.node/max-provisioning-failures
              :cimi.deployment.node/cpu.nb
              :cimi.deployment.node/ram.GB
              :cimi.deployment.node/disk.GB]}))

(s/def :cimi.deployment.runtime-parameter/mapped-to :cimi.core/nonblank-string)

(s/def :cimi.deployment.node/runtime-parameter (su/only-keys-maps
                                                 {:opt-un [:cimi.deployment.parameter/description
                                                           :cimi.deployment.parameter/default-value
                                                           :cimi.deployment.parameter/user-choice-value
                                                           :cimi.deployment.runtime-parameter/mapped-to]}))

(s/def :cimi.deployment/runtime-parameters
  (su/constrained-map keyword? :cimi.deployment.node/runtime-parameter))

(s/def :cimi.deployment/node (su/only-keys-maps {:req-un [:cimi.deployment/parameters]
                                                 :opt-un [:cimi.deployment/runtime-parameters]}))

(s/def :cimi.deployment/nodes (su/constrained-map keyword? :cimi.deployment/node))

(def deployment-attrs {:req-un [:cimi.deployment/id
                                :cimi.deployment/module-resource-uri
                                :cimi.deployment/type
                                :cimi.deployment/category
                                :cimi.deployment/mutable
                                :cimi.deployment/nodes]
                       :opt-un [:cimi.deployment/start-time
                                :cimi.deployment/end-time
                                :cimi.deployment/last-state-change-time
                                :cimi.deployment/state]})

(s/def :cimi/deployment (su/only-keys-maps c/common-attrs deployment-attrs))
