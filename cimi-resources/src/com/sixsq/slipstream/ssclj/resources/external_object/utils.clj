(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clojure.set :as set]
    [clj-time.coerce :as tc]
    [clj-time.core :as t])
  (:import
    (com.amazonaws.auth BasicAWSCredentials AWSStaticCredentialsProvider)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GeneratePresignedUrlRequest DeleteObjectRequest)
    (com.amazonaws.regions Regions)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws HttpMethod)))


(defn get-s3-client
  []
  (let [access "aws_access_key_id"
        secret "aws_secret_access_key"
        endpoint "aws_endpoint"
        config {:client-config {:protocol          "https"
                                :signature-version '"s3v4"}}
        file "/.aws/credentials"
        creds (-> "user.home"
                  System/getProperty
                  (str file)
                  slurp
                  (.split "\n"))

        cred (merge
               (set/rename-keys
                 (reduce
                   (fn [m e]
                     (let [pair (.split e "=")]
                       (if (some #{access secret endpoint} [(first pair)])
                         (apply assoc m pair)
                         m)))
                   {}
                   creds)
                 {access   :access-key
                  secret   :secret-key
                  endpoint :endpoint})
               config)
        endpoint (AwsClientBuilder$EndpointConfiguration. (:endpoint cred) "us-east-1")
        credentials (BasicAWSCredentials. (:access-key cred)
                                          (:secret-key cred))
        ]
    (-> (AmazonS3ClientBuilder/standard)
        (.withEndpointConfiguration endpoint)
        (.withCredentials (AWSStaticCredentialsProvider. credentials))
        .build)))

(def ^:const default-content-type "text/plain;charset=UTF-8")
(def ^:const default-ttl 15)

(defn generate-url
  ([bucket key] (generate-url bucket key :get))
  ([bucket key verb] (generate-url bucket key verb default-ttl))
  ([bucket key verb mn] (generate-url bucket key verb mn default-content-type))
  ([bucket key verb mn contentType]
   (let [expiration (tc/to-date (-> mn t/minutes t/from-now))
         generate-presigned-url-request (doto (GeneratePresignedUrlRequest. bucket key)
                                          (.setMethod (if (= verb :put)
                                                        HttpMethod/PUT
                                                        HttpMethod/GET))
                                          (.setExpiration expiration)
                                          (.setContentType contentType))]
     (.toString (.generatePresignedUrl (get-s3-client) generate-presigned-url-request)))))

(defn delete-s3-object [bucket key]
  (let [ deleteRequest (DeleteObjectRequest. bucket key)]
    (.deleteObject (get-s3-client) deleteRequest)))



