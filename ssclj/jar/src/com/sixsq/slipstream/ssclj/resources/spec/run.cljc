(ns com.sixsq.slipstream.ssclj.resources.spec.run
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.run/module-resource-uri :cimi.core/uri)
(s/def :cimi.run/type #{"Run" "Orchestration" "Machine"})
(s/def :cimi.run/category #{"Image" "Deployment"})
(s/def :cimi.run/start-time :cimi.core/timestamp)
(s/def :cimi.run/end-time :cimi.core/timestamp)
(s/def :cimi.run/last-state-change-time :cimi.core/timestamp)
(s/def :cimi.run/mutable boolean?)
(s/def :cimi.run/state #{"init" "provisioning" "executing" "sending report" "finalyzing" "terminated" "canceled" "done"})

(s/def :cimi.run.parameter/description string?)
(s/def :cimi.run.parameter/default-value string?)
(s/def :cimi.run.parameter/user-choice-value string?)

(s/def :cimi.run/parameter (su/only-keys-maps {:opt-un [:cimi.run.parameter/description
                                                        :cimi.run.parameter/default-value
                                                        :cimi.run.parameter/user-choice-value]}))

(s/def :cimi.run.node/cpu.nb :cimi.run/parameter)
(s/def :cimi.run.node/ram.GB :cimi.run/parameter)
(s/def :cimi.run.node/disk.GB :cimi.run/parameter)
(s/def :cimi.run.node/multiplicity :cimi.run/parameter)
(s/def :cimi.run.node/max-provisioning-failures :cimi.run/parameter)
(s/def :cimi.run.node/cloudservice :cimi.run/parameter)

(s/def :cimi.run/parameters
  (su/only-keys-maps
    {:req-un [:cimi.run.node/cloudservice]
     :opt-un [:cimi.run.node/multiplicity
              :cimi.run.node/max-provisioning-failures
              :cimi.run.node/cpu.nb
              :cimi.run.node/ram.GB
              :cimi.run.node/disk.GB]}))

(s/def :cimi.run.runtime-parameter/mapped-to :cimi.core/nonblank-string)

(s/def :cimi.run.node/runtime-parameter (su/only-keys-maps
                                          {:opt-un [:cimi.run.parameter/description
                                                    :cimi.run.parameter/default-value
                                                    :cimi.run.parameter/user-choice-value
                                                    :cimi.run.runtime-parameter/mapped-to]}))

(s/def :cimi.run/runtime-parameters
  (su/constrained-map keyword? :cimi.run.node/runtime-parameter))

(s/def :cimi.run/node (su/only-keys-maps {:req-un [:cimi.run/parameters]
                                          :opt-un [:cimi.run/runtime-parameters]}))

(s/def :cimi.run/nodes (su/constrained-map keyword? :cimi.run/node))

(def run-attrs {:req-un [#_:cimi.common/id                  ;TODO uncomment
                         :cimi.run/module-resource-uri
                         :cimi.run/type
                         :cimi.run/category
                         :cimi.run/mutable
                         :cimi.run/nodes]
                :opt-un [:cimi.run/start-time
                         :cimi.run/end-time
                         :cimi.run/last-state-change-time
                         :cimi.run/state]})

(s/def :cimi/run (su/only-keys-maps c/common-attrs run-attrs))
