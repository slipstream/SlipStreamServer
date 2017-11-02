(ns com.sixsq.slipstream.ssclj.resources.user-auto
  (:require
    [clj-time.core :as t]
    [com.sixsq.slipstream.ssclj.resources.spec.user-template-auto]
    [com.sixsq.slipstream.ssclj.resources.user :as p]
    [com.sixsq.slipstream.ssclj.resources.user-template-auto :as tpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/user-template.auto))
(defmethod p/validate-subtype tpl/registration-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-spec-validation-fn :cimi/user-template.auto-create))
(defmethod p/create-validate-subtype tpl/registration-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into user resource
;; strips method attribute and updates the resource URI
;;
(def epoch (u/unparse-timestamp-datetime (t/date-time 1970)))

(def ^:const initial-state "NEW")

(def user-auto-defaults
  {:isSuperUser false
   :state       initial-state
   :deleted     false
   :lastOnline  epoch
   :activeSince epoch
   :lastExecute epoch})

(defmethod p/tpl->user tpl/registration-method
  [resource request]
  (-> resource
      (assoc :resourceURI p/resource-uri)
      (merge user-auto-defaults)))

