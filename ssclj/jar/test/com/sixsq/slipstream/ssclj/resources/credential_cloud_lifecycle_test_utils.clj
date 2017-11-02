(ns com.sixsq.slipstream.ssclj.resources.credential-cloud-lifecycle-test-utils
    (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.credential :as credential]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]))


(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

(def base-uri (str p/service-context (u/de-camelcase credential/resource-url)))

(defn cloud-cred-lifecycle [{cloud-method-href :href :as credential-template-data}]
  (let [session-admin         (-> (session (ring-app))
                                  (content-type "application/json")
                                  (header authn-info-header "root ADMIN USER ANON"))
        session-user          (-> (session (ring-app))
                                  (content-type "application/json")
                                  (header authn-info-header "jane USER ANON"))
        session-anon          (-> (session (ring-app))
                                  (content-type "application/json")
                                  (header authn-info-header "unknown ANON"))

        name-attr             "name"
        description-attr      "description"
        properties-attr       {:a "one", :b "two"}

        href                  cloud-method-href
        template-url          (str p/service-context cloud-method-href)

        template              (-> session-admin
                                  (request template-url)
                                  (ltu/body->edn)
                                  (ltu/is-status 200)
                                  (get-in [:response :body]))
        create-import-no-href {:credentialTemplate (strip-unwanted-attrs template)}

        create-import-href    {:name               name-attr
                               :description        description-attr
                               :properties         properties-attr
                               :credentialTemplate credential-template-data}]

    ;; admin/user query should succeed but be empty (no credentials created yet)
    (doseq [session [session-admin session-user]]
      (-> session
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?)
          (ltu/is-operation-present "add")
          (ltu/is-operation-absent "delete")
          (ltu/is-operation-absent "edit")))

    ;; anonymous credential collection query should not succeed
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; creating a new credential without reference will fail for all types of users
    (doseq [session [session-admin session-user session-anon]]
      (-> session
          (request base-uri
                   :request-method :post
                   :body (json/write-str create-import-no-href))
          (ltu/body->edn)
          (ltu/is-status 400)))

    ;; creating a new credential as anon will fail
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str create-import-href))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; create a credential as a normal user
    (let [resp    (-> session-user
                      (request base-uri
                               :request-method :post
                               :body (json/write-str create-import-href))
                      (ltu/body->edn)
                      (ltu/is-status 201))
          id      (get-in resp [:response :body :resource-id])
          uri     (-> resp
                      (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; resource id and the uri (location) should be the same
      (is (= id uri))

      ;; admin/user should be able to see and delete credential
      (doseq [session [session-admin session-user]]
        (-> session
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 200)
            (ltu/is-operation-present "delete")
            (ltu/is-operation-absent "edit")))

      ;; ensure credential contains correct information
      (let [{:keys [name description properties
                    key secret tenant-name domain-name]} (-> session-user
                                                             (request abs-uri)
                                                             (ltu/body->edn)
                                                             (ltu/is-status 200)
                                                             :response
                                                             :body)]
        (is (= name name-attr))
        (is (= description description-attr))
        (is (= properties properties-attr))
        (is (= "key" key))
        (is (= "secret" secret))

        ;; delete the credential
        (-> session-user
            (request abs-uri
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))))))
