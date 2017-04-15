(ns com.sixsq.slipstream.ssclj.resources.spec.connector
  (:require
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.connector-template]))

(s/def :cimi/connector :cimi/connector-template)
