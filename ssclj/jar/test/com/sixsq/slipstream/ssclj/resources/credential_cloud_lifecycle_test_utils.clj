(ns com.sixsq.slipstream.ssclj.resources.credential-cloud-lifecycle-test-utils
  (:require
    [clojure.test :refer [deftest is are use-fixtures]]
    [peridot.core :refer :all]
    [clojure.data.json :as json]
    [clojure.string :as str]
    [cemerick.url :as url]
    [com.sixsq.slipstream.ssclj.resources.credential :as credential]
    [com.sixsq.slipstream.ssclj.resources.credential-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.common.dynamic-load :as dyn]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.credential.key-utils :as key-utils]
    [com.sixsq.slipstream.ssclj.resources.connector :as con]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as cont]))

(defn strip-unwanted-attrs
  [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description :properties}]
    (into {} (remove #(unwanted (first %)) m))))

(defn ring-app
  []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

(def base-uri (str p/service-context (u/de-camelcase credential/resource-url)))

; create connector instance
(defn get-connector-template
  [cloud-service-type connector-instance-name]
  (let [template-url (str p/service-context cont/resource-url "/" cloud-service-type)
        resp         (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "internal ADMIN")
                         (request template-url)
                         (ltu/body->edn)
                         (ltu/is-status 200))
        template     (get-in resp [:response :body])]
    {:connectorTemplate (-> template
                            strip-unwanted-attrs
                            (assoc :instanceName connector-instance-name))}))

(defn create-connector-instance
  [cloud-service-type connector-instance-name]
  (let [connector-create-uri (str p/service-context con/resource-url)
        href                 (str cont/resource-url "/" cloud-service-type)
        href-create          (get-connector-template cloud-service-type connector-instance-name)]
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "internal ADMIN")
        (request connector-create-uri
                 :request-method :post
                 :body (json/write-str href-create))
        (ltu/body->edn)
        (ltu/is-status 201)
        (ltu/location))))

(defn connector-instance-name
  [credential-template-data]
  (-> credential-template-data
      (get-in [:connector :href])
      (str/split #"/")
      (second)))

(defn cred-find
  [session conn-inst-name]
  (-> session
      (content-type "application/x-www-form-urlencoded")
      (request base-uri
               :request-method :put
               :body (url/map->query {:$filter  (format "type^='cloud-cred' and connector/href='connector/%s'" conn-inst-name)
                                      :$orderby "created:desc"
                                      :$last    1}))
      (ltu/body->edn)
      (ltu/is-status 200)))

(defn cred-create
  [session templ]
  (-> session
      (request base-uri
               :request-method :post
               :body (json/write-str templ))
      (ltu/body->edn)
      (ltu/is-status 201)))

(defn cred-take-first
  [response]
  (-> response
      (get-in [:response :body :credentials])
      first))

(defn cloud-cred-lifecycle
  [{cloud-method-href :href :as credential-template-data} cloud-service-type]
  (create-connector-instance cloud-service-type (connector-instance-name credential-template-data))
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
        (ltu/is-status 400))

    ;; create a credential as a normal user
    (let [resp    (-> session-user
                      (request base-uri
                               :request-method :post
                               :body (json/write-str create-import-href))
                      (ltu/body->edn)
                      (ltu/is-status 201))
          _       (ltu/refresh-es-indices)
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
            (ltu/is-operation-present "edit")))

      ;; ensure credential contains correct information
      (let [{:keys [name description
                    properties key secret]} (-> session-user
                                                (request abs-uri)
                                                (ltu/body->edn)
                                                (ltu/is-status 200)
                                                :response
                                                :body)]
        (is (= name name-attr))
        (is (= description description-attr))
        (is (= properties properties-attr))
        (is (= (get-in create-import-href [:credentialTemplate :key]) key))
        (is (= (get-in create-import-href [:credentialTemplate :secret]) secret))

        ;; delete the credential
        (-> session-user
            (request abs-uri
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))))

    ;; Editing is not allowed.  Hence only update, which is
    ;; - search / merge / create new / delete old
    (let [secret         {:secret (str "new" (:secret credential-template-data))}
          conn-inst-name (connector-instance-name credential-template-data)

          ;; CREATION
          ;; create initial cred
          id             (-> (cred-create session-user create-import-href)
                             (ltu/location))
          ;; another one as different user
          _              (cred-create session-admin create-import-href)
          _              (ltu/refresh-es-indices)

          ;; UPDATE OPERATION
          ;; search and get - only one is expected
          old-doc        (-> (cred-find session-user conn-inst-name)
                             (ltu/is-count 1)
                             cred-take-first)
          id-old         (:id old-doc)
          ;; merge
          new-doc        (-> (strip-unwanted-attrs old-doc)
                             (merge secret)
                             (assoc :href cloud-method-href))
          ;; create new
          _              (-> (cred-create session-user {:credentialTemplate new-doc})
                             (ltu/location))
          _              (ltu/refresh-es-indices)
          ;; test - search - two are expected
          _              (-> (cred-find session-user conn-inst-name)
                             (ltu/is-count 2))
          ;; delete old
          _              (-> session-user
                             (request (str p/service-context id-old)
                                      :request-method :delete)
                             (ltu/body->edn)
                             (ltu/is-status 200))
          _              (ltu/refresh-es-indices)]

      ;; VALIDATION
      (is (= (:secret secret) (-> (cred-find session-user conn-inst-name)
                                  (ltu/is-count 1)
                                  cred-take-first
                                  :secret))))))
