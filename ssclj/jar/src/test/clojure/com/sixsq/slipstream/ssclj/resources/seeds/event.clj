(ns com.sixsq.slipstream.ssclj.resources.seeds.event
  (:require
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [clj-time.core :as time]
    [com.sixsq.slipstream.ssclj.resources.event :as e]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :as aih]
    [com.sixsq.slipstream.ssclj.resources.test-utils :as tu]
    [com.sixsq.slipstream.ssclj.db.impl :as db]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.es.es-binding :as esb]))

(def base-uri (str p/service-context (u/de-camelcase e/resource-name)))

(defn- rnd-date-str
  []
  (-> 1000000 rand-int time/seconds time/from-now str))

(defn- event-template
  []
  {
   :acl       {
               :owner {
                       :type "USER" :principal :placeholder}
               :rules [{:type "USER" :principal :placeholder :right "ALL"}]}
   :timestamp (rnd-date-str)
   :content   {
               :resource {:href :placeholder}
               :state    "Started"}
   :type      "state"
   :severity  (rand-nth ["critical" "high" "medium" "low"])})

(defn events
  [nb-events username]
  (for [i (range nb-events)]
    (-> (event-template)
        (assoc-in [:acl :owner :principal] (name username))
        (assoc-in [:acl :rules 0 :principal] (name username))
        (assoc-in [:content :resource :href] (str "run/" i)))))


(defn insert-to-db
  [events username]
  (let [state (-> (session (tu/ring-app))
                  (content-type "application/json")
                  (header aih/authn-info-header username))]
    (doseq [event events]
      (request state base-uri
               :request-method :post
               :body (json/write-str event)))))

(defn seed!
  [nb-events username & {:keys [clean]}]
  (db/set-impl! (esb/get-instance))
  ;;(when clean)
    ;; TODO ES equivalent (kc/delete dbdb/resources))
  (-> nb-events
      (events username)
      (insert-to-db username)))
