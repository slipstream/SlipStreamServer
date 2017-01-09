(ns com.sixsq.slipstream.ssclj.resources.configuration-template.spec
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.common.spec :as c]))

(s/def ::service ::c/nonblank-string)

(defmulti service-type :service)

(defmethod service-type :default
  [resource]
  (throw (ex-info (str "unknown ConfigurationTemplate type: " (:service resource)) resource)))


(s/def ::configuration-template (s/multi-spec service-type :service))

