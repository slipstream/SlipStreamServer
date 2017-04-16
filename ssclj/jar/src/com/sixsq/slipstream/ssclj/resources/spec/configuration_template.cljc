(ns com.sixsq.slipstream.ssclj.resources.spec.configuration-template
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; Parameter definitions.

(s/def :cimi.configuration-template/service
  (su/regex-string #"[a-z0-9-]" #"^[a-z0-9]+(-[a-z0-9]+)*$"))

(s/def :cimi.configuration-template/href :cimi.core/resource-href) ;; FIXME: Ensure this always references the same resource type.

;;
;; Keys specifications for configuration-template resources.
;; As this is a "base class" for configuration-template resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def configuration-template-keys-spec {:req-un [:cimi.configuration-template/service]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs configuration-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        configuration-template-keys-spec
                        {:opt-un [:cimi.configuration-template/href]}]))

