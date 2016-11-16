(ns com.sixsq.slipstream.ssclj.resources.usage-summary-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.usage-summary :refer :all]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.app.routes :as routes]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(use-fixtures :each ltu/with-test-client-fixture)

(def base-uri (str p/service-context (u/de-camelcase resource-name)))

(defn ring-app []
  (ltu/make-ring-app (ltu/concat-routes [(routes/get-main-routes)])))


(deftest lifecycle

  (let [valid-create {:end-timestamp     "2016-10-15T00:00:00.000Z"
                      :user              "joe"
                      :cloud             "cloud-2"
                      :frequency         "monthly"
                      :usage-summary     "{:ram  {:unit-minutes 97185920}
                                          :disk {:unit-minutes 950.67}
                                          :cpu  {:unit-minutes 9250}
                                          }"
                      :acl               {
                                          :owner {:type "USER" :principal "joe"},
                                          :rules [{:type      "ROLE"
                                                   :principal "ADMIN"
                                                   :right      "ALL"
                                                   },
                                                  {
                                                   :type      "ROLE"
                                                   :principal "cloud-2"
                                                   :right     "ALL"
                                                   }, {
                                                       :type      "USER",
                                                       :principal "joe",
                                                       :right     "ALL"
                                                       }]
                                          },
                      :start-timestamp   "2016-11-14T00:00:00.000Z"
                      :grouping          "user,cloud"
                      }


        invalid-create (dissoc valid-create :frequency)]

    ;; anonymous create should fail
    (-> (session (ring-app))
        (content-type "application/json")
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; user create should also fail
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "jane USER")
        (request base-uri
                 :request-method :post
                 :body (json/write-str valid-create))
        (ltu/body->edn)
        (ltu/is-status 403))

    ;; admin create with invalid template fails
    (-> (session (ring-app))
        (content-type "application/json")
        (header authn-info-header "root ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    (let [uri (-> (session (ring-app))
                  (content-type "application/json")
                  (header authn-info-header "root ADMIN")
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin get succeeds
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; admin query succeeds
      (let [entries (-> (session (ring-app))
                        (content-type "application/json")
                        (header authn-info-header "root ADMIN")
                        (request base-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-resource-uri collection-uri)
                        (ltu/is-count #(= 1 %))
                        (ltu/entries resource-tag))]
        (is ((set (map :id entries)) uri))

        ;; verify that all entries are accessible
        (let [pair-fn (juxt :id #(str p/service-context (:id %)))
              pairs (map pair-fn entries)]
          (doseq [[id entry-uri] pairs]
            (-> (session (ring-app))
                (header authn-info-header "root ADMIN")
                (request entry-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-id id)))))

      ;; admin delete succeeds
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> (session (ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))

(deftest bad-methods
  (let [resource-uri (str p/service-context (u/new-resource-id resource-name))]
    (doall
      (for [[uri method] [[base-uri :options]
                          [base-uri :delete]
                          [base-uri :put]
                          [resource-uri :options]
                          [resource-uri :post]]]
        (-> (session (ring-app))
            (request uri
                     :request-method method
                     :body (json/write-str {:dummy "value"}))
            (ltu/is-status 405))))))

