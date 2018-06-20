(ns com.sixsq.slipstream.ssclj.resources.spec.module-image-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.connector :as t]  ;; FIXME: Change to module-version when available.
    [com.sixsq.slipstream.ssclj.resources.spec.module-image :as module-image]
    [expound.alpha :as expound]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id             (str t/resource-url "/connector-uuid")
              :resourceURI    t/resource-uri
              :created        timestamp
              :updated        timestamp
              :acl            valid-acl

              :os             "Ubuntu"
              :loginUser      "ubuntu"
              :sudo           true

              :cpu            2
              :ram            2048
              :disk           100
              :volatileDisk   500
              :networkType    "public"

              :connectors      [{:href "connector/some-cloud"
                                :imageID "my-great-image-1"}
                               {:href "connector/some-other-cloud"
                                :imageID "great-stuff"}]
              :connectorClasses [{:href "connector-template/some"
                                :imageID "my-great-image-1"}]
              :relatedImage   {:href "module/other"}}]

    (expound/expound ::module-image/module-image root)
    (is (s/valid? ::module-image/module-image root))
    (is (false? (s/valid? ::module-image/module-image (assoc root :badKey "badValue"))))
    (is (false? (s/valid? ::module-image/module-image (assoc root :os "BAD_OS"))))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :os :loginUser :networkType}]
      (is (false? (s/valid? ::module-image/module-image (dissoc root k)))))

    ;; optional attributes
    (doseq [k #{:connectors :connectorClasses :sudo :relatedImage
                :cpu :ram :disk :volatileDisk}]
      (is (true? (s/valid? ::module-image/module-image (dissoc root k)))))))
