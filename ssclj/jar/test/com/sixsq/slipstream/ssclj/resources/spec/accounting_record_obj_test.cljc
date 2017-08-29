(ns com.sixsq.slipstream.ssclj.resources.spec.accounting-record-obj-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]

    [com.sixsq.slipstream.ssclj.resources.spec.accounting-record-obj :as t]

    ;; FIXME: Reference real resource rather than connector template.
    [com.sixsq.slipstream.ssclj.resources.connector-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.spec.connector-template :as cts]

    [com.sixsq.slipstream.ssclj.util.spec :as su]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]}
  )


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id           (str ct/resource-url "/uuid")
              :resourceURI  ct/resource-uri
              :created      timestamp
              :updated      timestamp
              :acl          valid-acl

              ;; common accounting record attributes
              :type         "vm"
              :start        timestamp
              :stop         timestamp
              :user         "some-complicated-user-id"
              :cloud        "my-cloud"
              :roles        ["a" "b" "c"]
              :groups       ["g1" "g2" "g3"]
              :realm        "my-organization"
              :module       "module/example/images/centos-7"
              :serviceOffer {:href "service-offer/my-uuid"}

              ;; object store subtype
              :size          2048
              }]
    (is (s/valid? :cimi/accounting-record.obj root))
    (doseq [k #{:size}]
      (is (not (s/valid? :cimi/accounting-record.obj (dissoc root k)))))

    (is (not (s/valid? :cimi/accounting-record.obj (merge root {:wrong :only-keys}))))


    )
  )


