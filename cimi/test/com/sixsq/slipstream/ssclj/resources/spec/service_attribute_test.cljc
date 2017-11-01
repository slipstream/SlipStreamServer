(ns com.sixsq.slipstream.ssclj.resources.spec.service-attribute-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.service-attribute :as sa]))

(def valid? (partial s/valid? :cimi/service-attribute))
(def invalid? (complement valid?))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(deftest check-attribute
  (let [timestamp "1964-08-25T10:00:00.0Z"
        attr {:id            (str sa/resource-url "/test-attribute")
              :name          "Test Attribute"
              :description   "A attribute containing a test value."
              :resourceURI   sa/resource-uri
              :created       timestamp
              :updated       timestamp
              :acl           valid-acl

              :prefix        "example-org"
              :attributeName "test-attribute"
              :type          "string"}]

    (are [expect-fn arg] (expect-fn arg)
                         valid? attr
                         invalid? (dissoc attr :name)
                         invalid? (dissoc attr :description)
                         invalid? (dissoc attr :created)
                         invalid? (dissoc attr :updated)
                         invalid? (dissoc attr :acl)

                         invalid? (dissoc attr :prefix)
                         invalid? (assoc attr :prefix 0)
                         invalid? (assoc attr :prefix "")

                         invalid? (dissoc attr :attributeName)
                         invalid? (assoc attr :attributeName 0)
                         invalid? (assoc attr :attributeName "")

                         invalid? (dissoc attr :type)
                         invalid? (assoc attr :type 0)
                         valid? (assoc attr :type "string"))))
