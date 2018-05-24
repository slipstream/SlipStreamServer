(ns com.sixsq.slipstream.ssclj.resources.callback-user-creation
  "Creates a new user resource presumably after some external authentication
   method has succeeded."
  (:require
    [com.sixsq.slipstream.auth.external :as external]
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [com.sixsq.slipstream.util.response :as r]))


(def ^:const action-name "user-creation")


(defmethod callback/execute action-name
  [{{:keys [href]} :targetResource data :data callback-id :id :as resource}]
  (let [info {:authn-login       "test-username"
              :authn-method      "github"
              :email             "email@example.com"
              :fail-on-existing? true}]
    (if-let [username (external/create-user-when-missing! info)]
      (let [resource-id (str "user/" username)]
        (utils/callback-succeeded! callback-id)
        (r/map-response "success" 200 resource-id))
      (let [id (:authn-login info)]
        (utils/callback-failed! callback-id)
        (log-util/log-and-throw 400 (format "executing action %s of %s FAILED" action-name id))))))
