(ns com.sixsq.slipstream.ssclj.resources.user-params-template-exec
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.spec.user-params-template-exec]
    [com.sixsq.slipstream.ssclj.resources.user-params-template :as p]
    [com.sixsq.slipstream.ssclj.resources.user-params :as uep]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.userparamsdesc :refer [slurp-exec-params-desc]])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const params-type "execution")

(def ^:const resource
  {:paramsType          params-type
   :defaultCloudService ""
   :keepRunning         "on-success"
   :mailUsage           "never"
   :verbosityLevel      0
   :timeout             30})

(def ^:const desc
  (merge p/UserParamTemplateDescription
         (slurp-exec-params-desc)))

;;
;; initialization: register this Configuration template
;;
(defn initialize
  []
  (p/register resource desc))

;;
;; multimethods for validation
;;

(def validate-fn (u/create-spec-validation-fn :cimi/user-params-template.exec))
(defmethod p/validate-subtype params-type
  [resource]
  (validate-fn resource))