(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t]
    [clojure.edn :as edn])
  (:import
    (com.amazonaws.auth BasicAWSCredentials AWSStaticCredentialsProvider)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model GeneratePresignedUrlRequest DeleteObjectRequest ResponseHeaderOverrides)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws HttpMethod)))


(def ^:const default-ttl 15)

(defn object-store-config
  []
  (-> "user.home"
      System/getProperty
      (str "/.credentials/object-store-conf.edn")
      slurp
      edn/read-string))

(defn get-s3-client
  []
  (let [{:keys [key secret objectStoreEndpoint]} (object-store-config)
        endpoint (AwsClientBuilder$EndpointConfiguration. objectStoreEndpoint "us-east-1")
        credentials (-> (BasicAWSCredentials. key secret)
                        (AWSStaticCredentialsProvider.))]
    (-> (AmazonS3ClientBuilder/standard)
        (.withEndpointConfiguration endpoint)
        (.withCredentials credentials)
        .build)))


(defn generate-url
  [bucket key verb & [{:keys [ttl content-type filename]}]]
  (let [expiration (tc/to-date (-> (or ttl default-ttl) t/minutes t/from-now))
        overrides (when filename
                    (doto (ResponseHeaderOverrides.)
                      (.setContentDisposition (format "attachment; filename=\"%s\"" filename))))
        method (if (= verb :put)
                 HttpMethod/PUT
                 HttpMethod/GET)

        req (doto (GeneratePresignedUrlRequest. bucket key)
              (.setMethod  method)
              (.setExpiration expiration))]

    ;; mutates the req object!
    (cond
      content-type (.setContentType req content-type)
      overrides (.setResponseHeaders req overrides))

    (str (.generatePresignedUrl (get-s3-client) req))))


(defn delete-s3-object [bucket key]
  (let [deleteRequest (DeleteObjectRequest. bucket key)]
    (.deleteObject (get-s3-client) deleteRequest)))


