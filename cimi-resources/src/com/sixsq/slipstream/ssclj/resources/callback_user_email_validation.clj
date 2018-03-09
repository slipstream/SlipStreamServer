(ns com.sixsq.slipstream.ssclj.resources.callback-user-email-validation
  "Verifies that the email address for a user is valid. On validation, the
   user state is changed from NEW to ACTIVE."
  (:require
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.util.response :as r]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.db.impl :as db])
  (:import (clojure.lang ExceptionInfo)))

(def ^:const action-name "user-email-validation")


(def ^:const admin-opts {:user-name "INTERNAL", :user-roles ["ADMIN"]})


(defn validated-email!
  [user-id]
  (try
    (-> (crud/retrieve-by-id user-id admin-opts)
        (u/update-timestamps)
        (assoc :state "ACTIVE")
        (db/edit admin-opts))
    (catch ExceptionInfo ei
      (ex-data ei))))


(defmethod callback/execute action-name
  [{{:keys [href]} :resource :as resource}]
  (let [{:keys [id state] :as user} (crud/retrieve-by-id href admin-opts)]
    (if (= "NEW" state)
      (let [msg (str "email for " id " successfully validated")]
        (validated-email! id)
        (log/info msg)
        (r/map-response msg 200 id))
      (log-util/log-and-throw 400 (format "%s is not in the 'NEW' state" id)))))
