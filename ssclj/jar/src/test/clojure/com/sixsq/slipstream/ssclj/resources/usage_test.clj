(ns com.sixsq.slipstream.ssclj.resources.usage-test
  (:require
    [clojure.test                                               :refer :all]
    [ring.middleware.json                                       :refer [wrap-json-body wrap-json-response]]
    [ring.middleware.params                                     :refer [wrap-params]]
    [clj-time.core                                              :as time]
    [korma.core                                                 :as kc]
                              
    [peridot.core                                               :refer :all]

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

(defn reset-summaries
  [f]
  (acl/-init)
  (kc/delete rc/usage_summaries)
  (kc/delete acl/acl)
  (f))

(use-fixtures :each reset-summaries)

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

(deftest extract-id-roles
  (is 
    (= ["john" ["exo1" "exo"]]
    (id-roles {:identity
                {:current "john", 
                 :authentications {
                  "john" 
                    {:identity "john", :roles ["exo1" "exo"]}}}}))))

(deftest get-without-authn-succeeds 
  (-> (session (ring-app))
      (content-type "application/json")      
      (request base-uri)      
      t/body->json      
      (t/is-status 200)))

(deftest get-with-authn-succeeds    
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "jane")
      (request base-uri)      
      t/body->json
      (t/is-status 200)))

(defn daily-summary 
  "convenience function"
  [user cloud [year month day] usage]
  { :user                user
    :cloud               cloud
    :start_timestamp     (u/timestamp year month day)
    :end_timestamp       (u/timestamp-next-day year month day)    
    :usage               usage })

(defn every-timestamps?   
  [pred? ts]  
  (every? (fn [[a b]] (pred? (u/to-time a) (u/to-time b))) (partition 2 1 ts)))

(defn are-desc-dates?   
  [m]
  (->> (get-in m [:response :body :usages])
       (map :end_timestamp)       
       (every-timestamps? (complement time/before?))       
       is)
  m)

(defn are-all-usages?   
  [m field expected]
  (->> (get-in m [:response :body :usages])
       (map field)       
       distinct
       (= [expected])              
       is)
  m)

(defn insert-summaries   
  []
  (rc/insert-summary! (daily-summary "joe" "exo"    [2015 04 16] {:ram { :unit_minutes 100.0}}))
  (rc/insert-summary! (daily-summary "joe" "exo"    [2015 04 17] {:ram { :unit_minutes 200.0}}))
  (rc/insert-summary! (daily-summary "mike" "aws"   [2015 04 18] {:ram { :unit_minutes 500.0}}))
  (rc/insert-summary! (daily-summary "mike" "exo"   [2015 04 16] {:ram { :unit_minutes 300.0}}))
  (rc/insert-summary! (daily-summary "mike" "aws"   [2015 04 17] {:ram { :unit_minutes 400.0}})))

(deftest get-should-return-most-recent-first-by-user

  (insert-summaries)

  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "joe")
      (request base-uri)      
      t/body->json       
      (t/is-key-value :count 2)
      are-desc-dates?
      (are-all-usages? :user "joe"))          

  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "mike") 
      (request base-uri)   
      t/body->json
      (t/is-key-value :count 3)
      are-desc-dates?
      (are-all-usages? :user "mike")))

(deftest acl-filter-cloud-with-role
  (insert-summaries)
  (-> (session (ring-app))
      (content-type "application/json")
      (header authn-info-header "john exo1 exo")      
      (request base-uri)      
      t/body->json      
      (t/is-key-value :count 3)
      are-desc-dates?
      (are-all-usages? :cloud "exo")))

(deftest get-uuid-with-correct-authn 
  (insert-summaries)
  (let [uuid (-> (kc/select rc/usage_summaries (kc/limit 1))
                 first
                 :id)]    
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "john exo")        
        (request (str c/service-context uuid))              
        t/body->json              
        (t/is-key-value :id uuid)
        (t/is-status 200))))

(deftest get-uuid-without-correct-authn 
  (insert-summaries)
  (let [uuid (-> (kc/select rc/usage_summaries (kc/limit 1))
                 first
                 :id)]    
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "jack")        
        (request (str c/service-context uuid))              
        t/body->json              
        (t/is-status 403))))

(deftest pagination-full 
  (insert-summaries)    
  (-> (session (ring-app))
      (content-type "application/json")      
      (header authn-info-header "mike")          
      (request (str base-uri "?offset=0&limit=10"))                
      t/body->json       
      (t/is-status 200)
      (t/is-key-value :count 3)))

(deftest pagination-only-one 
  (insert-summaries)    
  (-> (session (ring-app))
      (content-type "application/json")      
      (header authn-info-header "mike")          
      (request (str base-uri "?offset=1&limit=1"))                
      t/body->json                   
      (t/is-status 200)
      (t/is-key-value :count 1)))

(defn expect-pagination   
  [code query-strings]
  (doseq [query-string query-strings]
    (-> (session (ring-app))
      (content-type "application/json")      
      (header authn-info-header "mike")          
      (request (str base-uri query-string))                
      t/body->json                   
      (t/is-status code))))

(deftest pagination-wrong-query
  (insert-summaries) 
  (expect-pagination 400 
    [ "?offset=-1&limit=10"
      "?offset=1&limit=-10"
      "?offset=-1&limit=-10"
      "?offset=a&limit=10"]))

(deftest pagination-does-not-check-max-limit
  (insert-summaries) 
  (expect-pagination 200 
    [ "?offset=1&limit=1000000"]))
