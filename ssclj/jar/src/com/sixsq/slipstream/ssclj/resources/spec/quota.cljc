(ns com.sixsq.slipstream.ssclj.resources.spec.quota
  (:require
    [clojure.spec.alpha :as s]
    [instaparse.core :as insta]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.filter.parser :as parser]))


(defn valid-cimi-filter?
  [f]
  (try
    (not (insta/failure? (parser/parse-cimi-filter f)))
    (catch Throwable _ false)))


(s/def :cimi.quota/resource :cimi.core/nonblank-string)
(s/def :cimi.quota/selection valid-cimi-filter?)
(s/def :cimi.quota/aggregation :cimi.core/nonblank-string)
(s/def :cimi.quota/limit pos-int?)


(s/def :cimi/quota
  (su/only-keys-maps c/common-attrs
                     {:req-un [:cimi.quota/resource
                               :cimi.quota/selection
                               :cimi.quota/aggregation
                               :cimi.quota/limit]}))
