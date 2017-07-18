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
(s/def :cimi.run/parameters (su/constrained-map keyword? string?))
(s/def :cimi.run/user-choices (su/constrained-map keyword? string?))

(s/def :cimi/run
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.common/id
                               :cimi.run/module-resource-uri
                               :cimi.run/type
                               :cimi.run/category
                               :cimi.run/mutable
                               :cimi.run/parameters
                               :cimi.run/user-choices]
                      :opt-un [:cimi.run/start-time
                               :cimi.run/end-time
                               :cimi.run/last-state-change-time
                               :cimi.run/state]}))
