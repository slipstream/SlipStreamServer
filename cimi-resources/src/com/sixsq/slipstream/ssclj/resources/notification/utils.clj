(ns com.sixsq.slipstream.ssclj.resources.notification.utils
  (:require [clj-time.core :as time]
            [com.sixsq.slipstream.auth.acl :as acl]
            [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
            [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))


(def resource-name "Notification")

(def resource-url (u/de-camelcase resource-name))

(def resource-uri (str c/cimi-schema-uri resource-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "ALL"}]})

(def severity-critical "critical")
(def severity-high "high")
(def severity-medium "medium")
(def severity-low "low")

(def type-state "state")
(def type-alarm "alarm")
(def type-action "action")
(def type-system "system")


(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))


(defn create-notification
  [resource-href, message, acl,
   & {:keys [severity type],
      :or   {severity severity-medium
             type     type-state}}]
  (let [notification-map {:resourceURI resource-uri
                          :content     {:resource {:href resource-href}
                                        :state    message}
                          :severity    severity
                          :type        type
                          :timestamp   (u/unparse-timestamp-datetime (time/now))
                          :acl         acl}
        create-request {:params   {:resource-name resource-url}
                        :identity std-crud/internal-identity
                        :body     notification-map}]
    (add-impl create-request)))
