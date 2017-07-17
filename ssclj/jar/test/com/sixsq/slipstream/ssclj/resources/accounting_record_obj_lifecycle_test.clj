(ns com.sixsq.slipstream.ssclj.resources.accounting-record-obj-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.accounting-record :as acc]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clojure.spec.alpha :as s]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase acc/resource-url)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))


(defn strip-unwanted-attrs [m]
  (let [unwanted #{:id :resourceURI :acl :operations
                   :created :updated :name :description}]
    (into {} (remove #(unwanted (first %)) m))))

(deftest lifecycle
  (let [
        session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "root ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))

        ]

    ;; admin user collection query should succeed but be empty (no accounting records created yet)
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; normal user collection query should succeed but be empty (no credentials created yet)
    (-> session-user
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-absent "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; anonymous credential collection query should not succeed
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))





    ;; create a credential as a normal user
    (let [timestamp "1964-08-25T10:00:00.0Z"
          create-req {:id           (str acc/resource-url "/uuid")
                      :resourceURI  acc/resource-uri
                      :created      timestamp
                      :updated      timestamp


                      ;; common accounting record attributes
                      :type         "obj"
                      :identifier   "my-cloud-vm-47"
                      :start        timestamp
                      :stop         timestamp
                      :user         "jane"
                      :cloud        "my-cloud"
                      :roles        ["a" "b" "c"]
                      :groups       ["g1" "g2" "g3"]
                      :realm        "my-organization"
                      :module       "module/example/images/centos-7"
                      :serviceOffer {:href "service-offer/my-uuid"}

                      ;; objectstore subtype
                      :size         2048
                      }
          resp (-> session-admin
                   (request base-uri
                            :request-method :post
                            :body (json/write-str create-req))
                   (ltu/body->edn)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin should be able to see, edit, and delete credential
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-present "edit"))

      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/dump)
          (ltu/is-status 200)
          (ltu/is-operation-absent "delete")
          (ltu/is-operation-absent "edit"))


      ;;edit
      (-> session-admin
          (request base-uri
                   :request-method :put
                   :body (json/write-str create-req))
          (ltu/body->edn)
          (ltu/is-status 200))

      (-> session-admin
          (request abs-uri
                   :request-method :put
                   :body (json/write-str (assoc create-req :id id) ))
          (ltu/body->edn)
          (ltu/is-status 200))

      (-> session-user
          (request abs-uri
                   :request-method :put
                   :body (json/write-str (assoc create-req :id id) ))
          (ltu/body->edn)
          (ltu/is-status 403))

      (-> session-user
          (request base-uri
                   :request-method :put
                   :body (json/write-str create-req))
          (ltu/body->edn)
          (ltu/is-status 200))

      (-> session-user
          (request abs-uri
                   :request-method :put
                   :body (json/write-str create-req))
          (ltu/body->edn)
          (ltu/is-status 403))

      (-> session-anon
          (request base-uri
                   :request-method :put
                   :body (json/write-str create-req))
          (ltu/body->edn)
          (ltu/is-status 403))

      ;;delete
      (-> session-user
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 403))

      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))


      ;;should be deleted
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 404))


      )
    )

  )



(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id acc/resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))
