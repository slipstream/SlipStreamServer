(ns com.sixsq.slipstream.ssclj.resources.external-object-test
  (:require
    [clojure.test :refer :all]
    [clojure.string :as s]
    [com.sixsq.slipstream.ssclj.resources.external-object :as eo]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as s3])
  (:import (clojure.lang ExceptionInfo)))

(def s3-host "s3.cloud.com")
(def s3-endpoint (str "https://" s3-host))

(def my-cloud-creds {"credential/my-cred" {:key    "key"
                                           :secret "secret"}})

(def bucketname "bucket-name")
(def runUUID "1-2-3-4-5")
(def filename "component.1.tgz")
(def objectname "object/name")

(deftest test-upload-fn
  (with-redefs [s3/create-bucket! (fn [_ _] nil)
                eo/expand-cred (fn [cred-href _] (get my-cloud-creds (:href cred-href)))
                eo/connector-from-cred (fn [_ _] {:objectStoreEndpoint s3-endpoint})]

    (is (thrown-with-msg? ExceptionInfo (re-pattern (eo/error-msg-bad-state "upload" eo/state-new eo/state-ready))
                          (eo/upload-fn {:state eo/state-ready} {})))

    ;; generic external object
    (is (s/starts-with? (eo/upload-fn {:state           eo/state-new
                                       :contentType     "application/tar+gzip"
                                       :bucketName      bucketname
                                       :objectName      objectname
                                       :objectStoreCred {:href "credential/my-cred"}}
                                      {})
                        (format "https://%s.%s/%s?" bucketname s3-host objectname)))

    ;; external object report
    (is (s/starts-with? (eo/upload-fn {:state           eo/state-new
                                       :contentType     "application/tar+gzip"
                                       :bucketName      bucketname
                                       :objectStoreCred {:href "credential/my-cred"}
                                       :runUUID         runUUID
                                       :filename        filename}
                                      {})
                        (format "https://%s.%s/%s/%s?" bucketname s3-host runUUID filename)))))

(deftest test-download-fn
  (with-redefs [eo/expand-cred (fn [cred-href _] (get my-cloud-creds (:href cred-href)))
                eo/connector-from-cred (fn [_ _] {:objectStoreEndpoint s3-endpoint})]

    (is (thrown-with-msg? ExceptionInfo (re-pattern eo/ex-msg-download-bad-state)
                          (eo/download-fn {:state eo/state-new} {})))

    (is (s/starts-with? (eo/download-fn {:state           eo/state-ready
                                         :bucketName      bucketname
                                         :objectName      objectname
                                         :objectStoreCred {:href "credential/my-cred"}}
                                        {})
                        (format "https://%s.%s/%s?" bucketname s3-host objectname)))))