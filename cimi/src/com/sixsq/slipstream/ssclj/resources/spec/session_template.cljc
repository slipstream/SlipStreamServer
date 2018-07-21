(ns com.sixsq.slipstream.ssclj.resources.spec.session-template
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.ui-hints :as hints]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;; All session resources must have a 'method' attribute.
(s/def ::method ::cimi-core/identifier)

;; All session resources must have a 'instance' attribute that is used in
;; the template identifier.
(s/def ::instance ::cimi-core/identifier)

;; Restrict the href used to create sessions.
(def session-template-regex #"^session-template/[a-z]+(-[a-z]+)*$")
(s/def ::href (s/and string? #(re-matches session-template-regex %)))

;;
;; Keys specifications for SessionTemplate resources.
;; As this is a "base class" for SessionTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def session-template-keys-spec {:req-un [::method ::instance]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        hints/ui-hints-spec
                        session-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        hints/ui-hints-spec
                        session-template-keys-spec
                        {:req-un [::href]}]))

