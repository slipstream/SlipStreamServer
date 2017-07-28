(ns com.sixsq.slipstream.ssclj.resources.spec.run-parameter
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.run-parameter/run-href :cimi.core/nonblank-string)
(s/def :cimi.run-parameter/name :cimi.core/nonblank-string)
(s/def :cimi.run-parameter/value string?)
(s/def :cimi.run-parameter/node-name :cimi.core/nonblank-string)
(s/def :cimi.run-parameter/node-index pos-int?)
(s/def :cimi.run-parameter/description string?)

(s/def :cimi/run-parameter
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.run-parameter/run-href
                               :cimi.run-parameter/name]
                      :opt-un [:cimi.run-parameter/node-name
                               :cimi.run-parameter/node-index
                               :cimi.run-parameter/description
                               :cimi.run-parameter/value]}))