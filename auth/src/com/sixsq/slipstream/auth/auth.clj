(ns com.sixsq.slipstream.auth.auth
  (:refer-clojure :exclude [update])
  (:require
    [clojure.data.json :as json]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.internal :as ia]
    [com.sixsq.slipstream.auth.utils.sign :as sg]
    [com.sixsq.slipstream.auth.github :as gh]
    [com.sixsq.slipstream.auth.cyclone :as cy]
    [com.sixsq.slipstream.auth.utils.http :as uh]))

(defn dispatch-on-authn-method
  [request]
  (-> request
      (uh/param-value :authn-method)
      keyword
      (or :internal)))

(defmulti login dispatch-on-authn-method)

(defmethod login :internal
  [request]
  (ia/login request))

(defmethod login :github
  [_]
  (gh/login))

(defmethod login :cyclone
  [_]
  (cy/login))

(defn logout
  [_]
  (ia/logout))


