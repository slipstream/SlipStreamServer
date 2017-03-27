(ns com.sixsq.slipstream.ssclj.resources.service-attribute-schema-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.service-attribute :refer :all]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def non-nil? (complement nil?))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest check-localized-entry
  (let [entry {:name        "readable name"
               :description "localized description"
               :categories  ["a" "b"]}]

    (are [expect-fn arg] (expect-fn (s/check LocalizedEntry arg))
                         nil? entry
                         non-nil? (dissoc entry :name)
                         non-nil? (assoc entry :name 99)
                         non-nil? (assoc entry :name "")

                         non-nil? (dissoc entry :description)
                         non-nil? (assoc entry :description 99)
                         non-nil? (assoc entry :description "")

                         nil? (dissoc entry :categories)
                         non-nil? (assoc entry :categories [])
                         non-nil? (assoc entry :categories "bad-value")

                         non-nil? (assoc entry :bad "value"))))

(deftest check-localized-entries
  (let [entries {:en {:name        "keyword"
                      :description "localized description"
                      :categories  ["one" "two"]}
                 :fr {:name        "mot clé"
                      :description "phrase descriptif localisé"
                      :categories  ["un" "deux"]}}]

    (are [expect-fn arg] (expect-fn (s/check LocalizedEntries arg))
                         nil? entries
                         nil? (dissoc entries :en)
                         nil? (dissoc entries :fr)

                         non-nil? (assoc entries :en "bad-value")
                         non-nil? (assoc entries :fr "bad-value")
                         non-nil? (dissoc entries :en :fr)
                         non-nil? {})))

;; CompositeType checks

(deftest check-composite-type
  (are [expect-fn arg] (expect-fn (s/check CompositeType arg))
                       nil? ["type-one"]
                       nil? ["type-one" "type-two"]
                       non-nil? []
                       non-nil? [0]
                       non-nil? "bad-value"))

(deftest check-attribute
  (let [timestamp "1964-08-25T10:00:00.0Z"
        attr {:id            resource-name
              :resourceURI   p/service-context
              :created       timestamp
              :updated       timestamp
              :acl           valid-acl

              :prefix        "example-org"
              :attr-name     "test-attribute"

              :type          "string"
              :authority     "http://helix-nebula.eu"
              :major-version 2
              :minor-version 1
              :patch-version 0
              :normative     true
              :en            {:name        "keyword"
                              :description "localized description"
                              :categories  ["one" "two"]}}]

    (are [expect-fn arg] (expect-fn (s/check Attribute arg))
                         nil? attr
                         non-nil? (dissoc attr :created)
                         non-nil? (dissoc attr :updated)
                         non-nil? (dissoc attr :acl)

                         non-nil? (dissoc attr :prefix)
                         non-nil? (assoc attr :prefix 0)
                         non-nil? (assoc attr :prefix "")

                         non-nil? (dissoc attr :attr-name)
                         non-nil? (assoc attr :attr-name 0)
                         non-nil? (assoc attr :attr-name "")

                         non-nil? (dissoc attr :type)
                         non-nil? (assoc attr :type 0)
                         nil? (assoc attr :type ["ok" "values"])

                         nil? (dissoc attr :authority)
                         non-nil? (assoc attr :authority 0)

                         non-nil? (dissoc attr :major-version)
                         non-nil? (assoc attr :major-version "bad-value")
                         non-nil? (dissoc attr :minor-version)
                         non-nil? (assoc attr :minor-version "bad-value")
                         non-nil? (dissoc attr :patch-version)
                         non-nil? (assoc attr :patch-version "bad-value")

                         non-nil? (dissoc attr :normative)

                         non-nil? (assoc attr :en "bad-value"))))
