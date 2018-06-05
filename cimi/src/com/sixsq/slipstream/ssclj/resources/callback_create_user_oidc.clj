(ns com.sixsq.slipstream.ssclj.resources.callback-create-user-oidc
  "Creates a new OIDC user resource presumably after external authentication has succeeded."
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "user-oidc-creation")


(defn register-user [{{:keys [href]} :targetResource :as callback-resource} request]
  (log/debug "registering user with href " href)
    ;;TODO
  )

(defmethod callback/execute action-name
  [{callback-id :id :as callback-resource} request]
  (log/debug "Executing callback" callback-id)
  (try
    (if-let [resp (register-user callback-resource request)]
      resp
      (do
        (utils/callback-failed! callback-id)
        (r/map-response "could not create OIDC user" 400)))
    (catch Exception e
      (utils/callback-failed! callback-id)
      (or (ex-data e) (throw e)))))

