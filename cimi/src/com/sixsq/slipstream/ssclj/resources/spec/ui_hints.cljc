(ns com.sixsq.slipstream.ssclj.resources.spec.ui-hints
  "Attributes that can be used to provide visualization hints for browser (or
   other visual) user interfaces."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


;; Provides a group label that can be used to describe a group of related
;; templates/forms. This will be used to identify members of the group in
;; addition to providing the description.
(s/def ::group ::cimi-core/nonblank-string)


;; Provides a hint for the visualization order of different templates/forms.
(s/def ::order nat-int?)


;; Indicates whether the form associated with the template should be hidden
;; on browser UIs.  When this is false or not specified, the UI should
;; render the associated form.
(s/def ::hidden boolean?)


;; Associates an icon with the template/form.  The names are those from
;; the FontAwesome 5 icon collection.  The full list of supported icons
;; is available from the React Semantic UI documentation:
;; https://react.semantic-ui.com/elements/icon
(s/def ::icon ::cimi-core/nonblank-string)


;; Provides a redirect URI to be used on success. Browser UIs can use
;; this to provide a smoother workflow. If this is specified, the
;; underlying template processing will return a redirect in all cases.
(s/def ::redirectURI ::cimi-core/nonblank-string)


(def ui-hints-spec {:opt-un [::group ::order ::hidden ::icon ::redirectURI]})
