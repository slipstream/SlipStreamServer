(ns com.sixsq.slipstream.ssclj.resources.usage-volume-test  
  (:require
    [clojure.test                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [clj-time.core                                              :as time]
    [korma.core                                                 :as kc]
                              
    [peridot.core                                               :refer :all]

    [com.sixsq.slipstream.ssclj.resources.usage-test            :as ut]

    [com.sixsq.slipstream.ssclj.database.korma-helper           :as kh]  
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils    :as du]  
    [com.sixsq.slipstream.ssclj.resources.common.schema         :as c]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header    :refer [authn-info-header wrap-authn-info-header]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri             :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler    :refer [wrap-exceptions]]

    [com.sixsq.slipstream.ssclj.api.acl                         :as acl]
    [com.sixsq.slipstream.ssclj.db.impl                         :as db]
    [com.sixsq.slipstream.ssclj.db.database-binding             :as dbdb]
    [com.sixsq.slipstream.ssclj.usage.record-keeper             :as rc]
    [com.sixsq.slipstream.ssclj.usage.utils                     :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud           :as crud]
    [com.sixsq.slipstream.ssclj.resources.usage                 :refer :all]
    [com.sixsq.slipstream.ssclj.app.routes                      :as routes]   
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils  :as t]))

(use-fixtures :each ut/reset-summaries)

(def users  (map #(str "joe" %) (range)))
(def days   (u/days-after 2015))
(def clouds (map #(str "exo-" %) (range)))

(def base-uri (str c/service-context resource-name))

(defn make-ring-app [resource-routes]
  (db/set-impl! (dbdb/get-instance))
  (-> resource-routes
      wrap-exceptions
      wrap-base-uri
      wrap-params
      wrap-authn-info-header
      (wrap-json-body {:keywords? true})
      (wrap-json-response {:pretty true :escape-non-ascii true})))

(defn ring-app []
  (make-ring-app (t/concat-routes routes/final-routes)))  

(defn mass-populate-summaries
  []
  (doseq [user (take 5 users) day (take 5 days) cloud (take 5 clouds)]     
    (-> (ut/daily-summary user 
                          cloud 
                          ((juxt time/year time/month time/day) day) 
                          {:ram { :unit_minutes 100.0}})
        rc/insert-summary!)))
  
  
(deftest pagination 
  (mass-populate-summaries))

  ; (-> (session (ring-app))
  ;     (content-type "application/json")      
  ;     (header authn-info-header "joe0")          
  ;     (request (str base-uri "?offset=0&limit=100"))                
  ;     t/body->json       
  ;     (t/is-status 200)
  ;     (t/is-key-value :count 100)))
