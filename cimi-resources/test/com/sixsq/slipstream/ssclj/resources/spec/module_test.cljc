(ns com.sixsq.slipstream.ssclj.resources.spec.module-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.module :as t]
    [com.sixsq.slipstream.ssclj.resources.spec.module :as module]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          (str t/resource-url "/connector-uuid")
              :resourceURI t/resource-uri
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl
              :path        "a/b/c"
              :type        "IMAGE"
              :versions    [{:href "module-image/xyz"}
                            {:href "module-image/abc"}]
              :logo        {:href "external-object/xyz"}}]

    (is (true? (s/valid? ::module/module root)))
    (is (false? (s/valid? ::module/module (assoc root :badKey "badValue"))))
    (is (false? (s/valid? ::module/module (assoc root :type "BAD_VALUE"))))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :path :type :versions}]
      (is (false? (s/valid? ::module/module (dissoc root k)))))

    ;; optional attributes
    (doseq [k #{:logo}]
      (is (true? (s/valid? ::module/module (dissoc root k)))))))
