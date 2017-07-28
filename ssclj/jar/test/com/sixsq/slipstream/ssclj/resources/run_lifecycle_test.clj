(ns com.sixsq.slipstream.ssclj.resources.run-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as rc]
    [com.sixsq.slipstream.ssclj.resources.run :refer :all]
    [com.sixsq.slipstream.ssclj.resources.run-parameter :as rp]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.zk.run.utils :as ru]))

(use-fixtures :each t/with-test-client-fixture)

(use-fixtures :once t/setup-embedded-zk)

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def valid-entry
  {:id                  (str resource-url "/dfd34916-6ede-47f7-aaeb-a30ddecbba5b")
   :resourceURI         resource-uri
   :module-resource-uri "module/examples/tutorials/service-testing/system/1940"
   :category            "Deployment"
   :type                "Orchestration"
   :mutable             false
   :nodes               {:node1 {:parameters         {:cloudservice {:description       "p1 description" ;TODO only node name and  multiplicity are being used for now from all these parameters
                                                                     :default-value     "abc"
                                                                     :user-choice-value "ABC"}
                                                      :multiplicity {:default-value "1"}}
                                 :runtime-parameters {:p1 {:description       "p1 description"
                                                           :default-value     "abc"
                                                           :user-choice-value "ABC"
                                                           :mapped-to         "a"}}}
                         :node2 {:parameters {:cloudservice {:description   "param1 description"
                                                             :default-value "abc"}}}}})

(deftest create-run

  (let [session-admin-json (-> (session (ring-app))
                               (content-type "application/json")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-admin-form (-> (session (ring-app))
                               (content-type "application/x-www-form-urlencoded")
                               (header authn-info-header "super ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json"))]

    ;; adding, retrieving and  deleting entry as user should succeed
    (let [run-href (-> session-user
                       (request base-uri
                                :request-method :post
                                :body (json/write-str valid-entry))
                       (t/body->edn)
                       (t/is-status 201)
                       (t/location))
          abs-uri (str p/service-context (u/de-camelcase run-href))
          created-run (-> session-user
                          (request abs-uri)
                          (t/body->edn)
                          (t/is-status 200))]

      (is (not (uzk/exists (str ru/znode-separator run-href))))

      (let [start-uri (str p/service-context (t/get-op created-run "http://schemas.dmtf.org/cimi/2/action/start"))
            started-run (-> session-user
                            (request start-uri)
                            (t/body->edn)
                            (t/is-status 200))]

        (is (not (get-in created-run [:body :start-time])))
        (is (get-in started-run [:response :body :start-time]))

        (are [expected value] (= expected value)
                              "init" (uzk/get-data (str ru/znode-separator run-href "/state"))
                              "init" (uzk/get-data
                                       (str ru/znode-separator run-href "/" ru/nodes-txt "/node2/1/" "vmstate")))
        )

      )))

