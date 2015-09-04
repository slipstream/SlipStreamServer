(ns com.sixsq.slipstream.ssclj.resources.seeds.event
  (:require
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.event :as e]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.test-utils :as tu]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding :as dbdb]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

(def base-uri (str p/service-context (u/de-camelcase e/resource-name)))

(def event-template {
                  :acl {
                        :owner {
                                :type "USER" :principal :placeholder}
                        :rules [{:type "USER" :principal :placeholder :right "ALL"}]}
                  :timestamp "2015-01-16T08:05:00.0Z"
                  :content  {
                             :resource {:href :placeholder}
                             :state "Started"}
                  :type "state"
                  :severity "critical"
                  })

(defn events
  [nb-events username]
  (for [i (range nb-events)]
    (-> event-template
        (assoc-in [:acl :owner :principal]    (name username))
        (assoc-in [:acl :rules 0 :principal]  (name username))
        (assoc-in [:content :resource :href] (str "run/" i)))))


(defn insert-to-db
  [events username]
  (db/set-impl! (dbdb/get-instance))
  (dbdb/init-db)

  (let [state (-> (session (tu/ring-app))
                  (content-type "application/json")
                  (header aih/authn-info-header username))]
    (doseq [event events]
      (request state base-uri
               :request-method :post
               :body (json/write-str event)))))

(defn seed!
  [nb-events username]
  (-> nb-events
      (events username)
      (insert-to-db username)))
