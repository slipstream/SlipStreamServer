(ns com.sixsq.slipstream.ssclj.resources.spec.deployment-template
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]))

;; All deployment templates must indicate the method used to create the deployment.
(s/def :cimi.deployment-template/method :cimi.core/identifier)

(def deployment-template-regex #"^deployment-template/[a-zA-Z0-9]([a-zA-Z0-9_-]*[a-zA-Z0-9])?$")
(s/def :cimi.deployment-template/href (s/and string? #(re-matches deployment-template-regex %)))

;;
;; Keys specifications for deploymentTemplate resources.
;; As this is a "base class" for deploymentTemplate resources, there
;; is no sense in defining map resources for the resource itself.
;;

(def deployment-template-keys-spec {:req-un [:cimi.deployment-template/method]})

(def deployment-template-keys-spec-opt {:opt-un [:cimi.deployment-template/method]})

(def resource-keys-spec
  (su/merge-keys-specs [c/common-attrs
                        deployment-template-keys-spec]))

(def create-keys-spec
  (su/merge-keys-specs [c/create-attrs]))

;; subclasses MUST provide the href to the template to use
(def template-keys-spec
  (su/merge-keys-specs [c/template-attrs
                        deployment-template-keys-spec-opt]))

