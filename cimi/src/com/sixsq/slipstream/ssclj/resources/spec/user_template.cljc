(ns com.sixsq.slipstream.ssclj.resources.spec.user-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;; All user templates must indicate the method used to create the user.
(s/def ::method ::cimi-core/identifier)

;; All user template resources must have a 'instance' attribute that is used as
;; the template identifier.
(s/def :cimi.session-template/instance ::cimi-core/identifier)

(def user-template-regex #"^user-template/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")
(s/def ::href (s/and string? #(re-matches user-template-regex %)))

;;
;; Keys specifications for UserTemplate resources.
;; As this is a "base class" for UserTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def user-template-keys-spec {:req-un [::method
                                       ::instance]})

(def user-template-template-keys-spec {:req-un [::instance]
                                       :opt-un [::method]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        user-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

;; subclasses MUST provide the href to the template to use
(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        user-template-template-keys-spec]))

