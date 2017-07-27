(ns com.sixsq.slipstream.ssclj.resources.run-parameter-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as rc]
    [com.sixsq.slipstream.ssclj.resources.run-parameter :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.zk.run.utils :as ru]
    [com.sixsq.slipstream.ssclj.resources.zk.run.utils :as zkru]))

(use-fixtures :each t/with-test-client-fixture)

(use-fixtures :once t/setup-embedded-zk)

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def session-user (-> (session (ring-app))
                      (content-type "application/json")
                      (header authn-info-header "jane USER ANON")))

(deftest create-run-parameter-xyz
  (let [run-id "run/abc34916-6ede-47f7-aaeb-a30ddecbba5b"
        valid-entry {:run-id run-id :node-name "machine" :node-index 1 :name "xyz" :value "XYZ"}
        znode-path (zkru/run-parameter-znode-path valid-entry)
        run-parameter-id (-> session-user
                             (request base-uri
                                      :request-method :post
                                      :body (json/write-str valid-entry))
                             (t/body->edn)
                             (t/is-status 201)
                             (t/location))
        abs-uri (str p/service-context (u/de-camelcase run-parameter-id))
        created-run-parameter (-> session-user
                                  (request abs-uri)
                                  (t/body->edn)
                                  (t/is-status 200))]

    (is (= "XYZ" (uzk/get-data znode-path)))

    (-> session-user
        (request abs-uri :request-method :put
                 :body (json/write-str {:value "newvalue"}))
        (t/body->edn)
        (t/is-status 200))

    (is (= "newvalue" (uzk/get-data znode-path)) "run parameter can be updated")))

(deftest create-run-parameter-state
  (let [run-id "run/def34916-6ede-47f7-aaeb-a30ddecbba5b"
        valid-entry {:run-id run-id :name "state" :value "init"}
        run-parameter-id (-> session-user
                             (request base-uri
                                      :request-method :post
                                      :body (json/write-str valid-entry))
                             (t/body->edn)
                             (t/is-status 201)
                             (t/location))
        abs-uri (str p/service-context (u/de-camelcase run-parameter-id))
        created-run-parameter (-> session-user
                                  (request abs-uri)
                                  (t/body->edn)
                                  (t/is-status 200))]

    (is (= "init" (uzk/get-data (str ru/znode-separator run-id "/state"))) "znode state is created")

    (-> session-user
        (request abs-uri :request-method :put
                 :body (json/write-str {:value "newvalue"}))
        (t/body->edn)
        (t/is-status 400))

    (is (= "init" (uzk/get-data (str ru/znode-separator run-id "/state"))) "run parameter state can't be updated")))
