(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

; FIXME: see https://github.com/slipstream/SlipStreamServer/issues/1309
(s/def :cimi.credential-template.cloud/key string?)
(s/def :cimi.credential-template.cloud/secret string?)
;(s/def :cimi.credential-template.cloud/key :cimi.core/nonblank-string)
;(s/def :cimi.credential-template.cloud/secret :cimi.core/nonblank-string)
(s/def :cimi.credential-template.cloud/connector :cimi.common/resource-link)
(s/def :cimi.credential-template.cloud/quota (s/or :pos-int pos-int? :zero zero?))

(def credential-template-cloud-keys-spec
  {:req-un [:cimi.credential-template.cloud/key
            :cimi.credential-template.cloud/secret
            :cimi.credential-template.cloud/connector
            :cimi.credential-template.cloud/quota]})

(def credential-template-create-keys-spec credential-template-cloud-keys-spec)
