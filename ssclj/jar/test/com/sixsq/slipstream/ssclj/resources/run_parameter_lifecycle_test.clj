(ns com.sixsq.slipstream.ssclj.resources.run-parameter-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [ring.util.codec :as rc]
    [com.sixsq.slipstream.ssclj.resources.run-parameter :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as t]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header create-identity-map]]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.util.zookeeper :as uzk]
    [com.sixsq.slipstream.ssclj.resources.zk.run.utils :as ru]
    [com.sixsq.slipstream.ssclj.resources.zk.run.utils :as zkru]))

(use-fixtures :each t/with-test-es-client-fixture)

(use-fixtures :once t/setup-embedded-zk)

(def base-uri (str p/service-context resource-url))

(defn ring-app []
  (t/make-ring-app (t/concat-routes routes/final-routes)))

(def session-user-jane (-> (session (ring-app))
                           (content-type "application/json")
                           (header authn-info-header "jane USER ANON")))

(def session-user-albert (-> (session (ring-app))
                             (content-type "application/json")
                             (header authn-info-header "albert USER ANON")))

(def identity-admin (create-identity-map ["super" #{"ADMIN"}]))

(def resource-acl-jane {:owner {:principal "ADMIN"
                                :type      "ROLE"}
                        :rules [{:principal "jane"
                                 :type      "USER"
                                 :right     "MODIFY"}]})

(deftest create-run-parameter-xyz
  (let [run-href "run/abc34916-6ede-47f7-aaeb-a30ddecbba5b"
        valid-entry {:run-href run-href :node-name "machine" :node-index 1 :type "node"
                     :name     "xyz" :value "XYZ" :acl resource-acl-jane}
        znode-path (zkru/run-parameter-znode-path valid-entry)
        run-parameter-id (->
                           (->> {:params   {:resource-name resource-url}
                                 :identity identity-admin
                                 :body     valid-entry}
                                (add-impl)
                                (assoc {} :response))
                           (t/is-status 201)
                           (t/location))
        abs-uri (str p/service-context (u/de-camelcase run-parameter-id))
        created-run-parameter (-> session-user-jane
                                  (request abs-uri)
                                  (t/body->edn)
                                  (t/is-status 200))]

    (is (= "XYZ" (uzk/get-data znode-path)))

    (-> session-user-jane
        (request abs-uri :request-method :put
                 :body (json/write-str {:value "newvalue"}))
        (t/body->edn)
        (t/is-status 200))

    (is (= "newvalue" (uzk/get-data znode-path)) "run parameter can be updated")

    (-> session-user-albert
        (request abs-uri :request-method :put
                 :body (json/write-str {:value "newvalue"}))
        (t/body->edn)
        (t/is-status 403))

    (is (not (= "newvalue-albert" (uzk/get-data znode-path))) "run parameter can be updated")))

