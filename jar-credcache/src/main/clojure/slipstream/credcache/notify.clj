(ns slipstream.credcache.notify
  "Utilities for notifying users via email."
  (:require
    [clojure.tools.logging :as log]
    [postal.core :as postal]
    [schema.core :as s]
    [clj-time.core :as t]
    [clj-time.coerce :as tc]
    [clj-time.format :as tf]))

(def ^:dynamic *smtp* nil)

;;
;; smtp parameter schema
;;

(def SmtpParameters
  {:host                  s/Str
   (s/optional-key :port) s/Int
   (s/optional-key :from) s/Str
   :user                  s/Str
   :pass                  s/Str
   (s/optional-key :ssl)  s/Str})

(defn set-smtp-parameters!
  "Sets the SMTP parameters used to send mail as defined in the
   given map.  If the parameters are not valid, an exception will
   be thrown."
  [m]
  (s/validate SmtpParameters m)
  (->> m
       (constantly)
       (alter-var-root #'*smtp*)))

(def fmt-renewal-failure-msg "
An attempt to renew your credential on the SlipStream server
has failed.  The renewal be tried again if there is sufficient
time left on the current credential (usually > 6 min).

The parameters for this credential are:

id     = %s
type   = %s
expiry = %s

Please be sure that appropriate credentials are available (for
example on the MyProxy server) for renewal.  If the credential
expires, then you will need to initialize a new credential in
your user account via the SlipStream server.
")

(defn get-from-value
  []
  (if *smtp*
    (or (:from *smtp*)
        (str (:user *smtp*) "@" (:host *smtp*)))))

(defn get-body
  [{:keys [id subtypeURI expiry]}]
  (let [expiry-date (if expiry
                      (tf/unparse (tf/formatters :basic-date-time)
                                  (tc/from-long (* 1000 expiry)))
                      "unknown")]
    (format fmt-renewal-failure-msg id subtypeURI expiry-date)))

(defn renewal-failure
  [{:keys [id email] :as credential}]
  (if (and *smtp* email)
    (let [msg {:from    (get-from-value)
               :to      [email]
               :subject "SlipStream credential renewal failure"
               :body    (get-body credential)}
          result (postal/send-message *smtp* msg)]
      (if-not (zero? (:code result))
        (log/error "error sending notification:" id (:error result) (:message result))
        (log/info "notification sent for renewal failure:" id)))))
