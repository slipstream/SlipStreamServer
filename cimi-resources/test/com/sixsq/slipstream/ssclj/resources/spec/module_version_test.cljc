(ns com.sixsq.slipstream.ssclj.resources.spec.module-version-test
  (:require
    [clojure.test :refer [deftest is are]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.module-version :as t]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "test"
                         :type      "USER"
                         :right     "VIEW"}]})

(def timestamp "1964-08-25T10:00:00.0Z")

(def valid-module-version {:id          "module-version/my-uuid-string"
                           :resourceURI t/resource-uri
                           :created     timestamp
                           :updated     timestamp
                           :acl         valid-acl
                           :name        "short-name"
                           :description "short description",
                           :properties  {:a "one", :b "two"}

                           :parent      "my/module/parent"
                           :path        "my/module/parent/short-name"
                           :type        "project"})

(deftest test-schema-check
  (are [expect-fn arg] (expect-fn (s/valid? :cimi/module-version arg))
                       true? valid-module-version
                       true? (assoc valid-module-version :ok-attr "a")
                       true? (assoc valid-module-version :ok-attr 1)
                       true? (assoc valid-module-version :ok-attr 1.0)
                       true? (assoc valid-module-version :ok-attr true)
                       true? (assoc valid-module-version :ok-attr 4/5)
                       false? (assoc valid-module-version "bad-attr" {})
                       false? (assoc valid-module-version "bad-attr" "test"))

  ;; mandatory keywords
  (doseq [k #{:id :resourceURI :created :updated :acl
              :name :parent :path :type}]
    (is (not (s/valid? :cimi/module-version (dissoc valid-module-version k)))))

  ;; optional keywords
  (doseq [k #{:description :properties}]
    (is (s/valid? :cimi/module-version (dissoc valid-module-version k)))))
