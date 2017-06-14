(ns com.sixsq.slipstream.ssclj.resources.session-template-cyclone
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.session-template-cyclone]
    [com.sixsq.slipstream.ssclj.resources.session-template :as p]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def ^:const authn-method "cyclone")

;;
;; description
;;
(def ^:const desc
  (merge p/SessionTemplateDescription))

;;
;; initialization: register this Session template
;;
(defn initialize
  []
  (p/register authn-method desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/session-template.cyclone))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))
