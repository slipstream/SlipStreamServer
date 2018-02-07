(ns com.sixsq.slipstream.ssclj.resources.external-object.utils-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.ssclj.resources.external-object.utils :as u]
            [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:use [amazonica.aws.s3]
        [amazonica.core])
  (:import java.util.UUID))


(defcredential (:access-key u/cred)
               (:secret-key u/cred)
               (:endpoint u/cred))

(defn- fetch-content
  "makes an HTTP request and fetches the binary object"
  [url]
  (let [req (client/get url)]
    (if (= (:status req) 200)
      (:body req))))

(deftest lifecycle
  []
  (let
    [
     nb (count (list-buckets))
     bucket-test (-> (UUID/randomUUID)
                     str
                     create-bucket
                     :name)
     key-test "key-test"
     write-url (u/generate-url bucket-test key-test 15 true)
     read-url (u/generate-url bucket-test key-test 15)
     payload "Sample content to be uploaded and then read"
     doc {:body payload :content-type "text/plain"}
     write-response (client/put write-url doc)]

    ;; create a bucket
    (is (not( nil? bucket-test)))

    ;;number of buckets should be incremented
    (is (= (inc nb)
           (count (list-buckets))))

    ;; upload was OK
    (is (= 200 (:status write-response)))

    ;;reading the s3 bucket is OK
    (is (= payload (fetch-content read-url)))

    ;;cleanup
    ;;
    (delete-object bucket-test key-test)

    (delete-bucket bucket-test)


    ;; check bucket is really gone
    (is (= nb
           (count (list-buckets))))))