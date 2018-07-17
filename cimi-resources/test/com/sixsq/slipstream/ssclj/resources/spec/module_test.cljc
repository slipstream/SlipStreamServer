(ns com.sixsq.slipstream.ssclj.resources.spec.module-test
  (:require
    [clojure.test :refer [deftest is are]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.module :as t]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "test"
                         :type      "USER"
                         :right     "VIEW"}]})


(def timestamp "1964-08-25T10:00:00.0Z")


(def valid-module {:id          "module/my-uuid-string"
                   :resourceURI t/resource-uri
                   :created     timestamp
                   :updated     timestamp
                   :acl         valid-acl
                   :name        "short-name"
                   :description "short description",
                   :properties  {:a "one", :b "two"}

                   :parent      "my/module/parent"
                   :path        "my/module/parent/short-name"
                   :type        "project"

                   :versions    [{:href "module-version/my-uuid-version-1"}
                                 {:href "module-version/my-uuid-version-2"}]})


(deftest test-schema-check
  (are [expect-fn arg] (expect-fn (s/valid? :cimi/module arg))
                       true? valid-module
                       true? (assoc valid-module :ok-attr "a")
                       true? (assoc valid-module :ok-attr 1)
                       true? (assoc valid-module :ok-attr 1.0)
                       true? (assoc valid-module :ok-attr true)
                       true? (assoc valid-module :ok-attr 4/5)
                       false? (assoc valid-module "bad-attr" {})
                       false? (assoc valid-module "bad-attr" "test")
                       false? (assoc valid-module :versions []))

  ;; mandatory keywords
  (doseq [k #{:id :resourceURI :created :updated :acl
              :name :parent :path :type :versions}]
    (is (not (s/valid? :cimi/module (dissoc valid-module k)))))

  ;; optional keywords
  (doseq [k #{:description :properties}]
    (is (s/valid? :cimi/module (dissoc valid-module k)))))
