(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t]
    [clojure.edn :as edn])
  (:import
    (com.amazonaws.auth BasicAWSCredentials AWSStaticCredentialsProvider)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GeneratePresignedUrlRequest DeleteObjectRequest)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws HttpMethod)))


(def ^:const default-ttl 15)

(defn object-store-config
  []
  (-> "user.home"
      System/getProperty
      (str "/.credentials/object-store-conf.edn")
      slurp
      (edn/read-string)))

(defn get-s3-client
  []
  (let [os-conf     (object-store-config)
        endpoint    (AwsClientBuilder$EndpointConfiguration. (:objectStoreEndpoint os-conf) "us-east-1")
        credentials (BasicAWSCredentials. (:key os-conf)
                                          (:secret os-conf))]
    (-> (AmazonS3ClientBuilder/standard)
        (.withEndpointConfiguration endpoint)
        (.withCredentials (AWSStaticCredentialsProvider. credentials))
        .build)))


(defn generate-url
  ([bucket key] (generate-url bucket key :get))
  ([bucket key verb] (generate-url bucket key verb default-ttl))
  ([bucket key verb mn] (generate-url bucket key verb mn nil))
  ([bucket key verb mn contentType]
   (let [expiration                     (tc/to-date (-> mn t/minutes t/from-now))
         req                            (doto (GeneratePresignedUrlRequest. bucket key)
                                          (.setMethod (if (= verb :put)
                                                        HttpMethod/PUT
                                                        HttpMethod/GET))
                                          (.setExpiration expiration))
         generate-presigned-url-request (if contentType
                                          (doto req (.setContentType contentType))
                                          req)]
     (str
       (.generatePresignedUrl
         (get-s3-client)
         generate-presigned-url-request)))))


(defn delete-s3-object [bucket key]
  (let [deleteRequest (DeleteObjectRequest. bucket key)]
    (.deleteObject (get-s3-client) deleteRequest)))


