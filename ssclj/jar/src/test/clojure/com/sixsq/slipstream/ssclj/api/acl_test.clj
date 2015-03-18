(ns com.sixsq.slipstream.ssclj.api.acl-test
  (:require    
    [com.sixsq.slipstream.ssclj.api.acl :as acl]     
    [clojure.test :refer :all]))

(defn init-db
  [f]
  (acl/-init)
  (f))

(defn clean-database
  [f]
  (acl/delete-all)
  (f))
  
(use-fixtures :once init-db)
(use-fixtures :each clean-database)

(deftest parse-authn
  (is (=  [["user"  "joe"] ["role" "USER"]]
          (acl/parse-authn  { "current" "joe"
                                  "authentications" {
                                    "joe" { "identity" "joe" "roles" ["USER"]}
                                    "rob" { "identity" "rob"}}}))))

(deftest getResourceIds-should-check-arguments  
  (is  (thrown? IllegalArgumentException (acl/-getResourceIds "run" {})))
  (is  (thrown? IllegalArgumentException (acl/-getResourceIds "run" nil))))  

(deftest insert-allow-redundant-but-no-duplicates-in-db

  (is (= 1 (acl/-insertResource "run/abc" "run" {:identity "joe"})))
  (is (= 1 (acl/-insertResource "run/abc" "run" {:identity "joe"})))

  (is (= 1 (count (acl/-getResourceIds "run" {:identity "joe"})))))

(deftest getResourceIds-should-filter-by-type-user-and-role
  (acl/-insertResource "run/1" "run" {:identity "joe" :roles ["ROLE1"]})
  (acl/-insertResource "run/2" "run" {:identity "rob" :roles []})
  (acl/-insertResource "run/3" "run" {:identity "meb"})

  (is (empty? (acl/-getResourceIds "image" {:identity "joe" :roles ["ROLE1"]})))

  (is (empty? (acl/-getResourceIds "run" {:identity "mick"})))
  (is (empty? (acl/-getResourceIds "run" {:identity "mick" :roles ["ROLE2"]})))

  (is (= #{"run/1"} (acl/-getResourceIds "run" {:identity "joe"})))    
  (is (= #{"run/1"} (acl/-getResourceIds "run" {:identity "mick" :roles ["ROLE1"]}))))

(deftest getResourceId-should-filter-by-id-type-user-and-role
  (acl/-insertResource "run/1" "run" {:identity "joe" :roles ["ROLE1"]})  

  (is (= false (acl/-hasResourceId "run/2" "run" {:identity "joe"})))
  (is (= false (acl/-hasResourceId "run/1" "image" {:identity "joe"})))
  (is (= false (acl/-hasResourceId "run/1" "run" {:identity "mick"})))
  (is (= false (acl/-hasResourceId "run/1" "run" {:identity "mick" :roles ["ROLE2"]})))  

  (is (= true (acl/-hasResourceId "run/1" "run" {:identity "joe"})))
  (is (= true (acl/-hasResourceId "run/1" "run" {:identity "mick" :roles ["ROLE1"]})))
  )  

(deftest delete-without-parameters-should-raise-exception
  (is  (thrown? IllegalArgumentException (acl/-deleteResource "run/1" "run" nil)))
  (is  (thrown? IllegalArgumentException (acl/-deleteResource "run/1" "run" {}))))

(deftest delete-resource-should-remove-access-to-given-user-and-role
  (acl/-insertResource "run/1" "run" {:identity "joe"})
  (acl/-insertResource "run/1" "run" {:identity "mick" :roles ["ROLE1"]})    
  
  (acl/-deleteResource "run/1" "run" {:identity "joe"})

  (is  (empty?  (acl/-getResourceIds "run" {:identity "joe"})))
  (is  (= #{"run/1"}  (acl/-getResourceIds "run" {:identity "mick"})))
  (is  (= #{"run/1"}  (acl/-getResourceIds "run" {:identity "alfred" :roles ["ROLE1"]})))  

  (acl/-deleteResource "run/1" "run" {:identity "mick" :roles ["ROLE1"]})
  
  (is (empty? (acl/-getResourceIds "run" {:identity "joe" :roles ["ROLE1"]})))
  (is (empty? (acl/-getResourceIds "run" {:identity "mick" :roles ["ROLE1"]}))))  
