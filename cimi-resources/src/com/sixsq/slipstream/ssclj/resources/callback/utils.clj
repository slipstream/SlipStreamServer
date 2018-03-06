(ns com.sixsq.slipstream.ssclj.resources.callback.utils
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.app.persistent-db :as pdb])
  (:import (clojure.lang ExceptionInfo)))


(defn executable?
  [{:keys [state expires]}]
  (and (= state "WAITING")
       (u/not-expired? expires)))


(defn update-callback-state!
  [state callback-id]
  (try
    (let [admin-opts {:user-name "INTERNAL", :user-roles ["ADMIN"]}]
      (-> (crud/retrieve-by-id callback-id admin-opts)
          (u/update-timestamps)
          (assoc :state state)
          (pdb/edit admin-opts)))
    (catch ExceptionInfo ei
      (ex-data ei))))


(def callback-succeeded! (partial update-callback-state! "SUCCEEDED"))


(def callback-failed! (partial update-callback-state! "FAILED"))
