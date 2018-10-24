(ns com.sixsq.slipstream.ssclj.resources.resource-metadata-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as t]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.spec.resource-metadata-test :as resource-metadata]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context t/resource-url))

(def valid-acl {:owner {:principal "ADMIN",
                        :type      "ROLE"},
                :rules [{:principal "ADMIN",
                         :type      "ROLE",
                         :right     "MODIFY"},
                        {:principal "ANON",
                         :type      "ROLE",
                         :right     "VIEW"}]})

(deftest lifecycle

  (let [session-anon (-> (ltu/ring-app)
                         session
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")]

    ;; admin query succeeds but is empty
    ;; admin can add new entries
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; user and anon queries succeed but are empty
    (doseq [session [session-user session-anon]]
      (-> session
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?)
          (ltu/is-operation-absent "add")
          (ltu/is-operation-absent "delete")
          (ltu/is-operation-absent "edit")))

    ;; user and anon create requests must fail
    (doseq [session [session-user session-anon]]
      (-> session
          (request base-uri
                   :request-method :post
                   :body (json/write-str (dissoc resource-metadata/valid :acl)))
          (ltu/body->edn)
          (ltu/is-status 403)))

    ;; check resource metadata creation
    (let [admin-uri (-> session-admin
                        (request base-uri
                                 :request-method :post
                                 :body (json/write-str (dissoc resource-metadata/valid :acl)))
                        (ltu/body->edn)
                        (ltu/is-status 201)
                        (ltu/location))
          admin-abs-uri (str p/service-context (u/de-camelcase admin-uri))]

      ;; everyone should see the created resource
      (doseq [session [session-admin session-user session-anon]]
        (-> session
            (request base-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-resource-uri t/collection-uri)
            (ltu/is-count 1)))

      ;; verify contents
      (let [{:keys [id typeURI] :as metadata} (-> session-admin
                                                  (request admin-abs-uri)
                                                  (ltu/body->edn)
                                                  (ltu/is-status 200)
                                                  (ltu/is-operation-present "edit")
                                                  (ltu/is-operation-present "delete")
                                                  :response
                                                  :body)]

        (is typeURI)
        (is (= (cu/document-id id) (cu/md5 typeURI))))

      ;; user and anon cannot delete resource metadata
      (doseq [session [session-user session-anon]]
        (-> session
            (request admin-abs-uri :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 403)))

      ;; admin can delete the resource metadata
      (-> session-admin
          (request admin-abs-uri :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id t/resource-url))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
