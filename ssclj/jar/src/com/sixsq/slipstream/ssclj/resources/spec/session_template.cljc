(ns com.sixsq.slipstream.ssclj.resources.spec.session-template
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; All session resources must have a 'method' attribute.
(s/def :cimi.session-template/method :cimi.core/identifier)

(def session-template-regex #"^session-template/[a-z]+(-[a-z]+)*$")
(s/def :cimi.session-template/href (s/and string? #(re-matches session-template-regex %)))

;;
;; Keys specifications for SessionTemplate resources.
;; As this is a "base class" for SessionTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def session-template-keys-spec {:req-un [:cimi.session-template/method]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs session-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        session-template-keys-spec
                        {:opt-un [:cimi.session-template/href]}]))

