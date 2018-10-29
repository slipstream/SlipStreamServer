(ns com.sixsq.slipstream.ssclj.resources.module-lifecycle-test
  (:require
    [clojure.data.json :as json]
    [clojure.test :refer [are deftest is use-fixtures]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.resources.module :as module]
    [peridot.core :refer :all]))


(use-fixtures :each ltu/with-test-server-fixture)


(def base-uri (str p/service-context (u/de-camelcase module/resource-name)))


(def timestamp "1964-08-25T10:00:00.0Z")


(def valid-entry {:id          (str module/resource-url "/connector-uuid")
                  :resourceURI module/resource-uri
                  :created     timestamp
                  :updated     timestamp
                  :parentPath  "a/b"
                  :path        "a/b/c"
                  :type        "IMAGE"})

(def valid-image {:os           "Ubuntu"
                  :loginUser    "ubuntu"
                  :sudo         true

                  :cpu          2
                  :ram          2048
                  :disk         100
                  :volatileDisk 500
                  :networkType  "public"

                  :imageIDs     {:some-cloud       "my-great-image-1"
                                 :some-other-cloud "great-stuff"}

                  :author       "someone"
                  :commit       "wip"})


(deftest lifecycle

  (let [session-anon (-> (session (ltu/ring-app))
                         (content-type "application/json"))
        session-admin (header session-anon authn-info-header "super ADMIN USER ANON")
        session-user (header session-anon authn-info-header "jane USER ANON")]

    ;; create: NOK for anon
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str (assoc valid-entry :content valid-image)))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; queries: NOK for anon
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 403))

    (doseq [session [session-admin session-user]]
      (-> session
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-count zero?)))


    ;; invalid module type
    (-> session-admin
        (request base-uri
                 :request-method :post
                 :body (json/write-str (assoc valid-entry
                                         :content valid-image
                                         :type "bad-module-type")))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; adding, retrieving and  deleting entry as user should succeed
    (doseq [session [session-admin session-user]]
      (let [uri (-> session
                    (request base-uri
                             :request-method :post
                             :body (json/write-str (assoc valid-entry :content valid-image)))
                    (ltu/body->edn)
                    (ltu/is-status 201)
                    (ltu/location))

            abs-uri (str p/service-context (u/de-camelcase uri))]

        ;; retrieve: NOK for anon
        (-> session-anon
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 403))

        (let [content (-> session-admin
                          (request abs-uri)
                          (ltu/body->edn)
                          (ltu/is-status 200)
                          :response
                          :body
                          :content)]
          (is (= valid-image (select-keys content (keys valid-image)))))

        ;; edit: NOK for anon
        (-> session-anon
            (request abs-uri
                     :request-method :put
                     :body (json/write-str (assoc valid-entry :content valid-image)))
            (ltu/body->edn)
            (ltu/is-status 403))

        ;; insert 5 more versions
        (doseq [_ (range 5)]
          (-> session-admin
              (request abs-uri
                       :request-method :put
                       :body (json/write-str (assoc valid-entry :content valid-image)))
              (ltu/body->edn)
              (ltu/is-status 200)))


        (let [versions (-> session-admin
                           (request abs-uri
                                    :request-method :put
                                    :body (json/write-str (assoc valid-entry :content valid-image)))
                           (ltu/body->edn)
                           (ltu/is-status 200)
                           :response
                           :body
                           :versions)]
          (is (= 7 (count versions)))

          ;; extract by indexes or last
          (doseq [[i n] [["_0" 0] ["_1" 1] ["" 6]]]
            (let [content-id (-> session-admin
                                 (request (str abs-uri i))
                                 (ltu/body->edn)
                                 (ltu/is-status 200)
                                 :response
                                 :body
                                 :content
                                 :id)]
              (is (= (-> versions (nth n) :href) content-id))
              (is (= (-> versions (nth n) :author) "someone"))
              (is (= (-> versions (nth n) :commit) "wip")))))

        (doseq [i ["_0" "_1"]]
          (-> session-admin
              (request (str abs-uri i)
                       :request-method :delete)
              (ltu/body->edn)
              (ltu/is-status 200))

          (-> session-admin
              (request (str abs-uri i))
              (ltu/body->edn)
              (ltu/is-status 404)))


        ;; delete out of bound index should return 404
        (-> session-admin
            (request (str abs-uri "_50")
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 404))

        (-> session-admin
            (request (str abs-uri "_50"))
            (ltu/body->edn)
            (ltu/is-status 404))

        ;; delete: NOK for anon
        (-> session-anon
            (request abs-uri
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 403))

        (-> session-admin
            (request abs-uri
                     :request-method :delete)
            (ltu/body->edn)
            (ltu/is-status 200))

        ;; verify that the resource was deleted.
        (-> session-admin
            (request abs-uri)
            (ltu/body->edn)
            (ltu/is-status 404))))))


(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id module/resource-name))]
    (ltu/verify-405-status [[base-uri :options]
                            [base-uri :delete]
                            [resource-uri :options]
                            [resource-uri :post]])))
