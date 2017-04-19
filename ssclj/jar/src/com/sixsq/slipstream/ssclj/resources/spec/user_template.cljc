(ns com.sixsq.slipstream.ssclj.resources.spec.user-template
  (:require
    [clojure.spec :as s]
    [clojure.spec.gen :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; All user templates must indicate the method used to create the user.
(s/def :cimi.user-template/method :cimi.core/identifier)

(s/def :cimi.user-template/href :cimi.core/resource-href) ;; FIXME: Ensure this always references the same resource type.

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

