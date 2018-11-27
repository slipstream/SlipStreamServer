(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.util.log :as logu])
  (:import
    (com.amazonaws AmazonServiceException HttpMethod SdkClientException)
    (com.amazonaws.auth AWSStaticCredentialsProvider BasicAWSCredentials)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model CreateBucketRequest DeleteObjectRequest
                                     GeneratePresignedUrlRequest HeadBucketRequest)))


(def ^:const default-ttl 15)


(def request-admin {:identity                      {:current         "internal"
                                                    :authentications {"internal" {:roles    #{"ADMIN"}
                                                                                  :identity "internal"}}}
                    :sixsq.slipstream.authn/claims {:username "internal"
                                                    :roles    "ADMIN"}
                    :params                        {:resource-name "user"}
                    :route-params                  {:resource-name "user"}
                    :user-roles                    #{"ANON"}})


(defn log-aws-exception
  [amazon-exception]
  (let [status (.getStatusCode amazon-exception)
        message (.getMessage amazon-exception)]
    (logu/log-and-throw status message)))


(defn log-sdk-exception
  [sdk-exception]
  (logu/log-and-throw-400 (.getMessage sdk-exception)))


(defmacro try-catch-aws-fn
  ([body]
   `(try ~body
         (catch AmazonServiceException ae#
           (log-aws-exception ae#))
         (catch SdkClientException se#
           (log-sdk-exception se#))
         (catch Exception e#
           (logu/log-and-throw-400 (.getMessage e#))))))


(defn get-s3-client
  [{:keys [key secret endpoint]}]
  (let [endpoint (AwsClientBuilder$EndpointConfiguration. endpoint "us-east-1")
        credentials (AWSStaticCredentialsProvider. (BasicAWSCredentials. key secret))]
    (-> (AmazonS3ClientBuilder/standard)
        (.withEndpointConfiguration endpoint)
        (.withCredentials credentials)
        .build)))


(defn generate-url
  [obj-store-conf bucket obj-name verb & [{:keys [ttl content-type]}]]
  (let [expiration (tc/to-date (-> (or ttl default-ttl) t/minutes t/from-now))
        method (if (= verb :put)
                 HttpMethod/PUT
                 HttpMethod/GET)
        req (doto (GeneratePresignedUrlRequest. bucket obj-name)
              (.setMethod method)
              (.setExpiration expiration))]
    (cond
      content-type (.setContentType req content-type))
    (str (.generatePresignedUrl (get-s3-client obj-store-conf) req))))


(defn delete-s3-object
  "Mocked in unit tests. Externalized from function below to allow for
   exceptions to be caught."
  [s3client deleteRequest]
  (.deleteObject s3client deleteRequest))


(defn try-delete-s3-object [s3-creds bucket object]
  (let [deleteRequest (DeleteObjectRequest. bucket object)]
    (try-catch-aws-fn
      (delete-s3-object (get-s3-client s3-creds) deleteRequest))))


(defn bucket-exists?
  "Function mocked in unit tests"
  [s3client bucketName]
  (.doesBucketExist s3client bucketName))


(defn head-bucket
  "This method returns a HeadBucketResult if the bucket exists and you have
  permission to access it. Otherwise, the method will throw an
  AmazonServiceException with status code '404 Not Found' if the bucket does
  not exist, '403 Forbidden' if the user does not have access to the bucket, or
  '301 Moved Permanently' if the bucket is in a different region than the
  client is configured with"
  [s3client bucket]
  (.headBucket s3client (HeadBucketRequest. bucket)))


(defn try-head-bucket [s3client bucket]
  (try-catch-aws-fn (head-bucket s3client bucket)))


(defn create-bucket!
  "Caller should have checked that bucket does not exist yet. If creation
  fails, an Exception is thrown; otherwise true is returned. Mocked in unit
  tests."
  [s3client bucket-name]
  (.createBucket s3client (CreateBucketRequest. bucket-name))
  true)


(defn expand-cred
  "Returns credential document after expanding `href-obj-store-cred`
   credential href.

  Deriving objectType from request is not directly possible as this is a
  request on action resource. We would need to get resource id, load the
  resource and get objectType from it. Instead, requiring objectType as
  parameter. It should be known to the callers."
  [href-obj-store-cred]
  (std-crud/resolve-hrefs href-obj-store-cred request-admin true))


(defn bucket-creation-ok?
  "When the requested bucket doesn't exist and can't be created. The external
  object resource must not be created."
  [s3client bucketName]
  (try
    (or (bucket-exists? s3client bucketName)
        (create-bucket! s3client bucketName))
    (catch Exception e
      (log/error (format "Error when creating bucket %s: %s" bucketName (.getMessage e)))
      false)))


(defn uploadable-bucket?
  "When the bucket exists, but the user can't create the object. The external
  object resource must not be created."
  [obj-store-conf bucket]
  (let [s3client (get-s3-client obj-store-conf)]
    (try-head-bucket s3client bucket)                       ;;throw exception if not authorized
    true))


(defn format-creds-for-s3-api
  "Need objectType to dispatch on when loading credentials."
  [href-obj-store-cred]
  (let [{:keys [key secret connector]} (expand-cred href-obj-store-cred)]
    {:key      key
     :secret   secret
     :endpoint (:objectStoreEndpoint connector)}))


(defn ok-to-add-external-resource?
  "Determines if S3 conditions are met on S3 for the user to safely add an
  external object resource. If everything is OK, then the resource itself is
  returned. Otherwise an error response map is thrown"
  [{:keys [bucketName objectStoreCred] :as resource} request]
  (let [obj-store-conf (format-creds-for-s3-api objectStoreCred)
        s3client (get-s3-client obj-store-conf)]

    ;; When the requested bucket exists, but the user doesn't have permission to it :
    ;; The external object resource must not be created."
    (uploadable-bucket? obj-store-conf bucketName)          ;; Throws if the bucket can't be written to.

    (if (bucket-creation-ok? s3client bucketName)
      resource
      (logu/log-and-throw 503 (format "Unable to create the bucket %s" bucketName)))))
