(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t])
  (:import
    (com.amazonaws.auth BasicAWSCredentials AWSStaticCredentialsProvider)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GeneratePresignedUrlRequest DeleteObjectRequest)
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
  ([obj-store-conf bucket obj-name] (generate-url obj-store-conf bucket obj-name :get))
  ([obj-store-conf bucket obj-name verb] (generate-url obj-store-conf bucket obj-name verb default-ttl))
  ([obj-store-conf bucket obj-name verb ttl] (generate-url obj-store-conf bucket obj-name verb ttl nil))
  ([obj-store-conf bucket obj-name verb ttl contentType]
   (let [expiration (tc/to-date (-> ttl t/minutes t/from-now))
         req (doto (GeneratePresignedUrlRequest. bucket obj-name)
               (.setMethod (if (= verb :put)
                             HttpMethod/PUT
                             HttpMethod/GET))
               (.setExpiration expiration))
         generate-presigned-url-request (if contentType
                                          (doto req (.setContentType contentType))
                                          req)]
     (str
       (.generatePresignedUrl
         (get-s3-client obj-store-conf)
         generate-presigned-url-request)))))


(defn delete-s3-object [obj-store-conf bucket obj-name]
  (let [deleteRequest (DeleteObjectRequest. bucket obj-name)]
    (.deleteObject (get-s3-client obj-store-conf) deleteRequest)))


