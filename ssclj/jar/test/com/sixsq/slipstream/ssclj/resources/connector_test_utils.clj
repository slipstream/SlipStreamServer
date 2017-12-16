(ns com.sixsq.slipstream.ssclj.resources.connector-test-utils
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]

    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.connector :as con]
    [com.sixsq.slipstream.ssclj.resources.connector-template :as ct])
  (:import (clojure.lang ExceptionInfo)))

(def base-uri (str p/service-context (u/de-camelcase con/resource-name)))
(def tpl-base-uri (str p/service-context (u/de-camelcase ct/resource-name)))

(defn new-instance-name
  [cloud-service-type]
  (str cloud-service-type "-" (System/currentTimeMillis)))

;;
;; Tests.
;;

(defn connector-lifecycle
  [cloud-service-type]
  (let [href (str ct/resource-url "/" cloud-service-type)
        template-url (str p/service-context ct/resource-url "/" cloud-service-type)
        resp (-> (session (ltu/ring-app))
                 (content-type "application/json")
                 (header authn-info-header "root ADMIN")
                 (request template-url)
                 (ltu/body->edn)
                 (ltu/is-status 200))
        template (get-in resp [:response :body])
        valid-create {:connectorTemplate (-> template
                                             ltu/strip-unwanted-attrs
                                             (assoc :instanceName (new-instance-name cloud-service-type)))}
        href-create {:connectorTemplate {:href         href
                                         :instanceName (new-instance-name cloud-service-type)}}
        invalid-create (assoc-in valid-create [:connectorTemplate :invalid] "BAD")]

    ;; admin create with invalid template fails
    (-> (session (ltu/ring-app))
        (content-type "application/json")
        (header authn-info-header "root ADMIN")
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; full connector lifecycle as administrator should work
    (let [uri (-> (session (ltu/ring-app))
                  (content-type "application/json")
                  (header authn-info-header "root ADMIN")
                  (request base-uri
                           :request-method :post
                           :body (json/write-str valid-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]


      ;; create again with the same connector instance name should fail with 409
      (-> (session (ltu/ring-app))
          (content-type "application/json")
          (header authn-info-header "root ADMIN")
          (request base-uri
                   :request-method :post
                   :body (json/write-str (assoc valid-create :instanceName uri)))
          (ltu/body->edn)
          (ltu/is-status 400))

      ;; admin get succeeds
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; anonymous query fails
      (-> (session (ltu/ring-app))
          (request base-uri)
          (ltu/body->edn)
          (ltu/is-status 403))

      ;; admin query succeeds
      (let [entries (-> (session (ltu/ring-app))
                        (content-type "application/json")
                        (header authn-info-header "root ADMIN")
                        (request base-uri)
                        (ltu/body->edn)
                        (ltu/is-status 200)
                        (ltu/is-resource-uri con/collection-uri)
                        (ltu/is-count 1)
                        (ltu/entries con/resource-tag))]
        (is ((set (map :id entries)) uri))

        ;; verify that all entries are accessible
        (let [pair-fn (juxt :id #(str p/service-context (:id %)))
              pairs (map pair-fn entries)]
          (doseq [[id entry-uri] pairs]
            (-> (session (ltu/ring-app))
                (header authn-info-header "root ADMIN")
                (request entry-uri)
                (ltu/body->edn)
                (ltu/is-status 200)
                (ltu/is-id id)))))

      ;; admin delete succeeds
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))

    ;; abbreviated lifecycle using href to template instead of copy
    (let [uri (-> (session (ltu/ring-app))
                  (content-type "application/json")
                  (header authn-info-header "root ADMIN")
                  (request base-uri
                           :request-method :post
                           :body (json/write-str href-create))
                  (ltu/body->edn)
                  (ltu/is-status 201)
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; admin delete succeeds
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200))

      ;; ensure entry is really gone
      (-> (session (ltu/ring-app))
          (header authn-info-header "root ADMIN")
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 404)))))


(defn connector-template-is-registered
  [cloud-service-type]
  (let [id (str ct/resource-url "/" cloud-service-type)
        doc (crud/retrieve-by-id id)]
    (is (= id (:id doc)))))


(defn template-lifecycle
  [cloud-service-type]

  ;; Get all regististered connector templates.
  ;; There should be only one connector of this type.
  (let [session (session (ltu/ring-app))
        entries (-> session
                    (content-type "application/json")
                    (header authn-info-header "root ADMIN")
                    (request tpl-base-uri)
                    (ltu/body->edn)
                    (ltu/is-status 200)
                    (ltu/is-resource-uri ct/collection-uri)
                    (ltu/is-count pos?)
                    (ltu/is-operation-absent "add")
                    (ltu/is-operation-absent "delete")
                    (ltu/is-operation-absent "edit")
                    (ltu/is-operation-absent "describe")
                    (ltu/entries ct/resource-tag))
        ids (set (map :id entries))
        types (set (map :cloudServiceType entries))]
    (is (contains? ids (str ct/resource-url "/" cloud-service-type)))
    (is (contains? types cloud-service-type))

    ;; Get connector template and work with it.
    (let [entry (first (filter #(= cloud-service-type (:cloudServiceType %)) entries))
          ops (ltu/operations->map entry)
          href (get ops (c/action-uri :describe))
          entry-url (str p/service-context (:id entry))
          describe-url (str p/service-context href)

          entry-resp (-> session
                         (content-type "application/json")
                         (header authn-info-header "root ADMIN")
                         (request entry-url)
                         (ltu/is-status 200)
                         (ltu/body->edn))

          entry-body (get-in entry-resp [:response :body])

          desc (-> session
                   (content-type "application/json")
                   (header authn-info-header "root ADMIN")
                   (request describe-url)
                   (ltu/body->edn)
                   (ltu/is-status 200))
          desc-body (get-in desc [:response :body])]
      (is (nil? (get ops (c/action-uri :add))))
      (is (nil? (get ops (c/action-uri :edit))))
      (is (nil? (get ops (c/action-uri :delete))))
      (is (:cloudServiceType desc-body))
      (is (:acl desc-body))

      (is (thrown-with-msg? ExceptionInfo #".*resource does not satisfy defined schema.*" (crud/validate entry-body)))
      (is (crud/validate (assoc entry-body :instanceName (new-instance-name cloud-service-type)))))))
