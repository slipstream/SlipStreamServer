(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-parameter
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.deployment-parameter/deployment-href :cimi.common/resource-link)
(s/def :cimi.deployment-parameter/name :cimi.core/nonblank-string)
(s/def :cimi.deployment-parameter/value string?)
(s/def :cimi.deployment-parameter/node-name :cimi.core/nonblank-string)
(s/def :cimi.deployment-parameter/node-index pos-int?)
(s/def :cimi.deployment-parameter/description string?)
(s/def :cimi.deployment-parameter/type #{"deployment" "node" "node-instance"})

(s/def :cimi/deployment-parameter
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.deployment-parameter/deployment-href
                               :cimi.deployment-parameter/name
                               :cimi.deployment-parameter/type]
                      :opt-un [:cimi.deployment-parameter/node-name
                               :cimi.deployment-parameter/node-index
                               :cimi.deployment-parameter/description
                               :cimi.deployment-parameter/value]}))