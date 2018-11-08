(ns com.sixsq.slipstream.ssclj.resources.spec.module-test
  (:require
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.module :as t]
    [com.sixsq.slipstream.ssclj.resources.spec.module :as module]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


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
              :parentPath  "a/b"
              :path        "a/b/c"
              :type        "IMAGE"
              :versions    [{:href   "module-image/xyz"
                             :author "someone"
                             :commit "wip"}
                            nil
                            {:href "module-image/abc"}]
              :logo        {:href "external-object/xyz"}

              :acceptContentTypes ["application/json" "application/x-something"]
              :acceptProtocols    ["http+s3" "posix+nfs"]}]

    (stu/is-valid ::module/module root)
    (stu/is-invalid ::module/module (assoc root :badKey "badValue"))
    (stu/is-invalid ::module/module (assoc root :type "BAD_VALUE"))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :path :type}]
      (stu/is-invalid ::module/module (dissoc root k)))

    ;; optional attributes
    (doseq [k #{:logo :versions :acceptContentTypes :acceptProtocols}]
      (stu/is-valid ::module/module (dissoc root k)))))
