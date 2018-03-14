(ns com.sixsq.slipstream.ssclj.resources.callback-email-validation
  (:require
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.db.impl :as db])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const action-name "email-validation")


(def ^:const admin-opts {:user-name "INTERNAL", :user-roles ["ADMIN"]})


(defn validated-email!
  [email-id]
  (try
    (-> (crud/retrieve-by-id email-id admin-opts)
        (u/update-timestamps)
        (assoc :validated? true)
        (db/edit admin-opts))
    (catch ExceptionInfo ei
      (ex-data ei))))


(defmethod callback/execute action-name
  [{{:keys [href]} :targetResource :as resource}]
  (let [{:keys [id validated?] :as email} (crud/retrieve-by-id href admin-opts)]
    (if-not validated?
      (let [msg (str id " successfully validated")]
        (validated-email! id)
        (log/info msg)
        (r/map-response msg 200 id))
      (log-util/log-and-throw 400 (format "%s already validated" id)))))
