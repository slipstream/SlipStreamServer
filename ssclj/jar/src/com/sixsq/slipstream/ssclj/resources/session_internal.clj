(ns com.sixsq.slipstream.ssclj.resources.session-internal
  (:require
    [clojure.string :as str]
    [com.sixsq.slipstream.ssclj.resources.session :as p]
    [com.sixsq.slipstream.ssclj.resources.session-template-internal :as tpl]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.auth.sign :as sg]
    [clj-time.format :as time-fmt]))

(def ^:const authn-method "internal")

;;
;; schemas
;;

(def SessionAttrs
  tpl/SessionTemplateAttrs)

(def Session
  p/Session)

(def SessionCreate
  (merge c/CreateAttrs
         {:sessionTemplate tpl/SessionTemplateRef}))

(def SessionDescription
  tpl/desc)

;;
;; description
;;
(def ^:const desc SessionDescription)

;;
;; multimethods for validation
;;

(def validate-fn (u/create-validation-fn Session))
(defmethod p/validate-subtype authn-method
  [resource]
  (validate-fn resource))

(def create-validate-fn (u/create-validation-fn SessionCreate))
(defmethod p/create-validate-subtype authn-method
  [resource]
  (create-validate-fn resource))

;;
;; transform template into session resource
;;
(defn create-session [{:keys [username]} headers]
  (let [virtual-host (:slipstream-ssl-server-hostname headers)
        client-ip (:x-real-ip headers)]
    (crud/new-identifier
      (cond-> {:authnMethod authn-method
               :username    username
               :expiry      (time-fmt/unparse (:date-time time-fmt/formatters) (sg/expiry-timestamp))}
              virtual-host (assoc :virtualHost virtual-host)
              client-ip (assoc :clientIP client-ip))
      p/resource-name)))

(defn cookie-header [cookie-name token]
  {:cookies {cookie-name {:value token, :path "/"}}})

(defn cookie-name [{:keys [id]}]
  (str "slipstream." (str/replace id "/" ".")))

(defmethod p/tpl->session authn-method
  [resource request]
  (let [headers (:headers request)
        credentials {:username (:username resource)
                     :password (:password resource)}
        [ok? token] (auth-internal/create-token credentials)]
    (when ok?
      (let [session (create-session credentials headers)]
        [(cookie-header (cookie-name session) token) session]))))
