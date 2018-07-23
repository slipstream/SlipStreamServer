(ns com.sixsq.slipstream.ssclj.resources.spec.credential-template-cloud
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.acl :as cimi-acl]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;; FIXME: see https://github.com/slipstream/SlipStreamServer/issues/1309
(s/def ::key string?)                                       ;; ::cimi-core/nonblank-string
(s/def ::secret string?)                                    ;; ::cimi-core/nonblank-string
(s/def ::connector ::cimi-common/resource-link)
(s/def ::quota nat-int?)
(s/def ::manager (su/only-keys :req-un [::cimi-acl/principal
                                        ::cimi-acl/type]))
(s/def ::managers (s/coll-of ::manager :min-count 1 :kind vector?))

(def credential-template-cloud-keys-spec
  {:req-un [::key
            ::secret
            ::connector
            ::quota]
   :opt-un [::managers]})

(def credential-template-create-keys-spec credential-template-cloud-keys-spec)
