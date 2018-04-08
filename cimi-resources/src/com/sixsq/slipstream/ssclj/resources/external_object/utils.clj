(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t])
  (:import
    (com.amazonaws.auth BasicAWSCredentials AWSStaticCredentialsProvider)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GeneratePresignedUrlRequest DeleteObjectRequest ResponseHeaderOverrides)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws HttpMethod)))


(def ^:const default-ttl 15)

(defn get-s3-client
  [{:keys [key secret endpoint]}]
  (let [endpoint (AwsClientBuilder$EndpointConfiguration. endpoint "us-east-1")
        credentials (-> (BasicAWSCredentials. key secret)
                        (AWSStaticCredentialsProvider.))]
    (-> (AmazonS3ClientBuilder/standard)
        (.withEndpointConfiguration endpoint)
        (.withCredentials credentials)
        .build)))

(defn generate-url
  [obj-store-conf bucket obj-name verb & [{:keys [ttl content-type filename]}]]
  (let [expiration (tc/to-date (-> (or ttl default-ttl) t/minutes t/from-now))
        overrides (when filename
                    (doto (ResponseHeaderOverrides.)
                      (.setContentDisposition (format "attachment; filename=\"%s\"" filename))))
        method (if (= verb :put)
                 HttpMethod/PUT
                 HttpMethod/GET)

        req (doto (GeneratePresignedUrlRequest. bucket obj-name)
              (.setMethod  method)
              (.setExpiration expiration))]

    (cond
      content-type (.setContentType req content-type)
      overrides (.setResponseHeaders req overrides))

    (str (.generatePresignedUrl (get-s3-client obj-store-conf) req))))

(defn delete-s3-object [obj-store-conf bucket obj-name]
  (let [deleteRequest (DeleteObjectRequest. bucket obj-name)]
    (.deleteObject (get-s3-client obj-store-conf) deleteRequest)))

