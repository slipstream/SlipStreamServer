(ns com.sixsq.slipstream.ssclj.resources.virtual-machine-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.virtual-machine :as vm]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.auth.internal :as auth-internal]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [clojure.spec.alpha :as s]))

(use-fixtures :each ltu/with-test-es-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase vm/resource-url)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))

(deftest lifecycle
  (let [session-admin (-> (session (ring-app))
                          (content-type "application/json")
                          (header authn-info-header "root ADMIN USER ANON"))
        session-user (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "jane USER ANON"))
        session-anon (-> (session (ring-app))
                         (content-type "application/json")
                         (header authn-info-header "unknown ANON"))]

    ;; admin user collection query should succeed but be empty (no  records created yet)
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; normal user collection query should succeed but be empty (no vm created yet)
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


    ;; create a vm as a normal user
    (let [timestamp "1964-08-25T10:00:00.0Z"
          create-req {:id           (str vm/resource-url "/uuid")
                      :resourceURI  vm/resource-uri
                      :created      timestamp
                      :updated      timestamp

                      :name         "short name"
                      :description  "short description",
                      :properties   {:a "one",
                                     :b "two"}

                      :instanceId   "aaa-bbb-111"
                      :state        "Running"
                      :ip           "127.0.0.1"


                      :cloud        {:href  "connector/0123-4567-8912",
                                     :roles ["realm:cern", "realm:my-accounting-group"]}


                      :run          {:href "run/aaa-bbb-ccc",
                                     :user {:href "user/test"}}

                      :serviceOffer {:href                  "service-offer/e3db10f4-ad81-4b3e-8c04-4994450da9e3"
                                     :resource:vcpu         1
                                     :resource:ram          4096
                                     :resource:disk         10
                                     :resource:instanceType "Large"}}

          create-priv-req (assoc create-req :run {:href "run/444-555-666"
                                                  :user {:href "user/jane"}})

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

          id (get-in resp [:response :body :resource-id])
          id-priv (get-in resp-priv [:response :body :resource-id])
          id-other (get-in resp-priv [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          uri-priv (-> resp-priv
                       (ltu/location))

          jane-uri (str p/service-context (u/de-camelcase uri))
          janet-uri (str p/service-context (u/de-camelcase uri-priv))]

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

      (-> session-user
          (request janet-uri)
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


      (-> session-user
          (request jane-uri
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

      (-> session-admin
          (request base-uri
                   :request-method :put
                   :body (json/write-str create-req))
          (ltu/body->edn)
          (ltu/is-count 2)
          (ltu/is-status 200))

      ;;delete
      (-> session-user
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
          (ltu/is-status 404)))))


(deftest bad-methods

  (let [resource-uri (str p/service-context (u/new-resource-id vm/resource-name))]
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
