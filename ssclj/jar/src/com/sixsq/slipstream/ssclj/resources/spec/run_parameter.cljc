(ns com.sixsq.slipstream.ssclj.resources.spec.run-parameter
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi/run-parameter
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.common/acl
                               :cimi.run-parameter/name
                               :cimi.run-parameter/run-id]}))
