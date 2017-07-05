(ns com.sixsq.slipstream.ssclj.resources.accounting-record-vm-lifecycle-test
    (:require
      [clojure.test :refer :all]
      [clojure.data.json :as json]
      [peridot.core :refer :all]
      [com.sixsq.slipstream.ssclj.resources.accounting_record :as acc]
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


               session-view-accounting (-> (session (ring-app))
                                           (content-type "application/json")
                                           (header authn-info-header (str "janet USER ANON my-organization:view_accounting")))

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


              ;;  user with view-accounting role collection query should succeed but be empty (no credentials created yet)
              (-> session-view-accounting
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
                                :type         "vm"
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

                                ;; vm subtype
                                :cpu          1
                                :ram          1024
                                :disk         10}
                    create-priv-req (assoc create-req :user "janet")
                    create-other-req (assoc create-req :user "karen" :realm "a-different-organization")
                    resp (-> session-admin
                             (request base-uri
                                      :request-method :post
                                      :body (json/write-str create-req))
                             (ltu/body->edn)
                             (ltu/is-status 201))
                    resp-priv (-> session-admin
                                  (request base-uri
                                           :request-method :post
                                           :body (json/write-str create-priv-req))
                                  (ltu/body->edn)
                                  (ltu/is-status 201))
                    resp-other (-> session-admin
                                   (request base-uri
                                            :request-method :post
                                            :body (json/write-str create-other-req))
                                   (ltu/body->edn)
                                   (ltu/is-status 201))
                    id (get-in resp [:response :body :resource-id])
                    id-priv (get-in resp-priv [:response :body :resource-id])
                    id-other (get-in resp-priv [:response :body :resource-id])
                    uri (-> resp
                            (ltu/location))
                    uri-priv (-> resp-priv
                                 (ltu/location))
                    uri-other (-> resp-other
                                  (ltu/location))
                    jane-uri (str p/service-context (u/de-camelcase uri))
                    janet-uri (str p/service-context (u/de-camelcase uri-priv))
                    karen-uri (str p/service-context (u/de-camelcase uri-other))
                    ]
                   ;; admin should be able to see everyone's records
                   (-> session-admin
                       (request jane-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-present "delete")
                       (ltu/is-operation-present "edit"))

                   (-> session-admin
                       (request janet-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-present "delete")
                       (ltu/is-operation-present "edit"))


                   (-> session-admin
                       (request karen-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-present "delete")
                       (ltu/is-operation-present "edit"))

                   ;;standard user should only see her own records
                   (-> session-user
                       (request jane-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-absent "delete")
                       (ltu/is-operation-absent "edit"))


                   (-> session-user
                       (request janet-uri)
                       (ltu/body->edn)
                       (ltu/is-status 403)
                       (ltu/is-operation-absent "delete")
                       (ltu/is-operation-absent "edit"))

                   (-> session-user
                       (request karen-uri)
                       (ltu/body->edn)
                       (ltu/is-status 403)
                       (ltu/is-operation-absent "delete")
                       (ltu/is-operation-absent "edit"))

                   ;; user with view-accounting role (janet) should see own , same realm (jane) but not external realms records (karen)
                   (-> session-view-accounting
                       (request jane-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-absent "delete")
                       (ltu/is-operation-absent "edit"))

                   (-> session-view-accounting
                       (request janet-uri)
                       (ltu/body->edn)
                       (ltu/is-status 200)
                       (ltu/is-operation-absent "delete")
                       (ltu/is-operation-absent "edit"))

                   (-> session-view-accounting
                       (request karen-uri)
                       (ltu/body->edn)
                       (ltu/is-status 403)
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
                       (request jane-uri
                                :request-method :put
                                :body (json/write-str (assoc create-req :id id)))
                       (ltu/body->edn)
                       (ltu/is-status 200))

                   (-> session-user
                       (request jane-uri
                                :request-method :put
                                :body (json/write-str (assoc create-req :id id)))
                       (ltu/body->edn)
                       (ltu/is-status 403))

                   (-> session-view-accounting
                       (request jane-uri
                                :request-method :put
                                :body (json/write-str (assoc create-req :id id)))
                       (ltu/body->edn)
                       (ltu/is-status 403))


                   (-> session-user
                       (request jane-uri
                                :request-method :put
                                :body (json/write-str create-req))
                       (ltu/body->edn)
                       (ltu/is-status 403))

                   ;;Queries
                   (-> session-user
                       (request base-uri
                                :request-method :put
                                :body (json/write-str create-req))
                       (ltu/body->edn)
                       (ltu/is-count 1)
                       (ltu/is-status 200))

                   (-> session-view-accounting
                       (request base-uri
                                :request-method :put
                                :body (json/write-str create-req))
                       (ltu/body->edn)
                       (ltu/is-count 2)
                       (ltu/is-status 200))

                   (-> session-anon
                       (request base-uri
                                :request-method :put
                                :body (json/write-str create-req))
                       (ltu/body->edn)
                       (ltu/is-status 403))

                   (-> session-admin
                       (request base-uri
                                :request-method :put
                                :body (json/write-str create-req))
                       (ltu/body->edn)
                       (ltu/is-count 3)
                       (ltu/is-status 200))

                   ;;delete
                   (-> session-user
                       (request jane-uri
                                :request-method :delete)
                       (ltu/body->edn)
                       (ltu/is-status 403))


                   (-> session-view-accounting
                       (request jane-uri
                                :request-method :delete)
                       (ltu/body->edn)
                       (ltu/is-status 403))

                   (-> session-admin
                       (request jane-uri
                                :request-method :delete)
                       (ltu/body->edn)
                       (ltu/is-status 200))

                   (-> session-admin
                       (request janet-uri
                                :request-method :delete)
                       (ltu/body->edn)
                       (ltu/is-status 200))



                   ;;record should be deleted
                   (-> session-admin
                       (request jane-uri
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
