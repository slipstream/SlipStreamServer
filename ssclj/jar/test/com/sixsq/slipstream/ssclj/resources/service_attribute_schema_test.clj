(ns com.sixsq.slipstream.ssclj.resources.service-attribute-schema-test
    (:require
    [com.sixsq.slipstream.ssclj.resources.service-attribute :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [entry {:name        "readable name"
             :description "localized description"
             :categories  ["a" "b"]}]

  (expect nil? (s/check LocalizedEntry entry))

  (expect (s/check LocalizedEntry (dissoc entry :name)))
  (expect (s/check LocalizedEntry (assoc entry :name 99)))
  (expect (s/check LocalizedEntry (assoc entry :name "")))

  (expect (s/check LocalizedEntry (dissoc entry :description)))
  (expect (s/check LocalizedEntry (assoc entry :description 99)))
  (expect (s/check LocalizedEntry (assoc entry :description "")))

  (expect nil? (s/check LocalizedEntry (dissoc entry :categories)))
  (expect (s/check LocalizedEntry (assoc entry :categories [])))
  (expect (s/check LocalizedEntry (assoc entry :categories "bad-value")))

  (expect (s/check LocalizedEntry (assoc entry :bad "value"))))

(let [entries {:en {:name        "keyword"
                    :description "localized description"
                    :categories  ["one" "two"]}
               :fr {:name        "mot clé"
                    :description "phrase descriptif localisé"
                    :categories  ["un" "deux"]}}]

  (expect nil? (s/check LocalizedEntries entries))

  (expect nil? (s/check LocalizedEntries (dissoc entries :en)))
  (expect nil? (s/check LocalizedEntries (dissoc entries :fr)))

  (expect (s/check LocalizedEntries (assoc entries :en "bad-value")))
  (expect (s/check LocalizedEntries (assoc entries :fr "bad-value")))

  (expect (s/check LocalizedEntries (dissoc entries :en :fr)))
  (expect (s/check LocalizedEntries {})))

;; CompositeType checks
(expect (s/check CompositeType []))
(expect nil? (s/check CompositeType ["type-one"]))
(expect nil? (s/check CompositeType ["type-one" "type-two"]))
(expect (s/check CompositeType [0]))
(expect (s/check CompositeType "bad-value"))

(let [timestamp "1964-08-25T10:00:00.0Z"
      attr {:id            resource-name
            :resourceURI   p/service-context
            :created       timestamp
            :updated       timestamp
            :acl           valid-acl

            :prefix         "example-org"
            :attr-name      "test-attribute"

            :type          "string"
            :authority     "http://helix-nebula.eu"
            :major-version 2
            :minor-version 1
            :patch-version 0
            :normative     true
            :en            {:name        "keyword"
                            :description "localized description"
                            :categories  ["one" "two"]}}]

  (expect nil? (s/check Attribute attr))
  (expect (s/check Attribute (dissoc attr :created)))
  (expect (s/check Attribute (dissoc attr :updated)))
  (expect (s/check Attribute (dissoc attr :acl)))

  (expect (s/check Attribute (dissoc attr :prefix)))
  (expect (s/check Attribute (assoc attr  :prefix 0)))
  (expect (s/check Attribute (assoc attr  :prefix "")))

  (expect (s/check Attribute (dissoc attr :attr-name)))
  (expect (s/check Attribute (assoc attr  :attr-name 0)))
  (expect (s/check Attribute (assoc attr  :attr-name "")))

  (expect (s/check Attribute (dissoc attr :type)))
  (expect (s/check Attribute (assoc attr :type 0)))
  (expect nil? (s/check Attribute (assoc attr :type ["ok" "values"])))

  (expect nil? (s/check Attribute (dissoc attr :authority)))
  (expect (s/check Attribute (assoc attr :authority 0)))

  (expect (s/check Attribute (dissoc attr :major-version)))
  (expect (s/check Attribute (assoc attr :major-version "bad-value")))
  (expect (s/check Attribute (dissoc attr :minor-version)))
  (expect (s/check Attribute (assoc attr :minor-version "bad-value")))
  (expect (s/check Attribute (dissoc attr :patch-version)))
  (expect (s/check Attribute (assoc attr :patch-version "bad-value")))

  (expect (s/check Attribute (dissoc attr :normative)))

  ;; TODO: This should not be allowed, but can't yet enforce this.
  ;;(expect (s/check Attribute (dissoc attr :en)))
  (expect (s/check Attribute (assoc attr :en "bad-value"))))
