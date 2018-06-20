(ns com.sixsq.slipstream.ssclj.resources.spec.module-image-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.connector :as t]  ;; FIXME: Change to module-version when available.
    [com.sixsq.slipstream.ssclj.resources.spec.module-image :as module-image]))


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

              :connector      {:href "connector/some-cloud"}
              :connectorClass "connector/some-cloud"
              :relatedImage   {:href "module/other"}}]

    (is (s/valid? ::module-image/module-image root))
    (is (false? (s/valid? ::module-image/module-image (assoc root :badKey "badValue"))))
    (is (false? (s/valid? ::module-image/module-image (assoc root :os "BAD_OS"))))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :os :loginUser :connector}]
      (is (false? (s/valid? ::module-image/module-image (dissoc root k)))))

    ;; optional attributes
    (doseq [k #{:connectorClass :sudo :relatedImage}]
      (is (true? (s/valid? ::module-image/module-image (dissoc root k)))))))
