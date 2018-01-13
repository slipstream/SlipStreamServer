(ns com.sixsq.slipstream.ssclj.resources.spec.user-params-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

(s/def :cimi.user-params-template/paramsType :cimi.core/nonblank-string)

(def user-params-template-keys-spec
  {:req-un [:cimi.user-params-template/paramsType]})

(def user-params-template-keys-spec-opt
  {:opt-un [:cimi.user-params-template/paramsType]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        user-params-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

;; subclasses MUST provide the href to the template to use
(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        user-params-template-keys-spec-opt]))

