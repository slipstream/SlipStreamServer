(ns com.sixsq.slipstream.ssclj.resources.email.utils
  (:require [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [com.sixsq.slipstream.ssclj.resources.callback-email-validation :as email-callback]
            [com.sixsq.slipstream.util.response :as r]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [postal.core :as postal]
            [com.sixsq.slipstream.ssclj.resources.callback :as callback])
  (:import (java.security MessageDigest)))


(def ^:const admin-opts {:user-name "INTERNAL", :user-roles ["ADMIN"]})


(def ^:const validation-email-body
  (str "To validate your email address, please visit the address:\n\n-->>    %s\n\n"
       "If you did not initiate this request, do NOT click on the\n"
       "link and report this to the service administrator."))


(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))


;; FIXME: Fix ugliness around needing to create ring requests with authentication!
(defn create-callback [email-id baseURI]
  (let [callback-request {:params   {:resource-name callback/resource-url}
                          :body     {:action         email-callback/action-name
                                     :targetResource {:href email-id}}
                          :identity {:current         "INTERNAL"
                                     :authentications {"INTERNAL" {:identity "INTERNAL"
                                                                   :roles    ["ADMIN"]}}}}
        {{:keys [resource-id]} :body status :status} (crud/add callback-request)]
    (if (= 201 status)
      (if-let [callback-resource (crud/set-operations (crud/retrieve-by-id resource-id admin-opts) {})]
        (if-let [validate-op (u/get-op callback-resource "execute")]
          (str baseURI validate-op)
          (let [msg "callback does not have execute operation"]
            (throw (ex-info msg (r/map-response msg 500 resource-id)))))
        (let [msg "cannot retrieve email validation callback"]
          (throw (ex-info msg (r/map-response msg 500 resource-id)))))
      (let [msg "cannot create email validation callback"]
        (throw (ex-info msg (r/map-response msg 500 email-id)))))))


(defn smtp-cfg
  "Extracts the SMTP configuration from the server's configuration resource.
   Note that this assumes a standard URL for the configuration resource."
  []
  (when-let [{:keys [mailHost mailPort mailSSL mailUsername mailPassword]} (crud/retrieve-by-id "configuration/slipstream" admin-opts)]
    {:host mailHost
     :port mailPort
     :ssl  mailSSL
     :user mailUsername
     :pass mailPassword}))


(defn send-validation-email [callback-url address]
  (try
    (let [smtp (smtp-cfg)]
      (let [sender (or (:user smtp) "administrator")
            body (format validation-email-body callback-url)
            msg {:from    sender
                 :to      [address]
                 :subject "email validation"
                 :body    body}
            resp (postal/send-message smtp msg)]
        (if-not (= :SUCCESS (:error resp))
          (let [msg (str "cannot send verification email: " (:message resp))]
            (throw (r/ex-bad-request msg))))))
    (catch Exception e
      (let [error-msg "server configuration for SMTP is missing"]
        (throw (ex-info error-msg (r/map-response error-msg 500)))))))
