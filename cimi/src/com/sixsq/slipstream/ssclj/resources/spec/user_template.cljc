(ns com.sixsq.slipstream.ssclj.resources.spec.user-template
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.user :as us]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; All user templates must indicate the method used to create the user.
(s/def :cimi.user-template/method :cimi.core/identifier)

(def user-template-regex #"^user-template/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")
(s/def :cimi.user-template/href (s/and string? #(re-matches user-template-regex %)))

;;
;; Keys specifications for UserTemplate resources.
;; As this is a "base class" for UserTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def user-template-keys-spec {:req-un [:cimi.user-template/method]})

(def user-template-keys-spec-opt {:opt-un [:cimi.user-template/method]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        user-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

;; subclasses MUST provide the href to the template to use
(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        user-template-keys-spec-opt]))

