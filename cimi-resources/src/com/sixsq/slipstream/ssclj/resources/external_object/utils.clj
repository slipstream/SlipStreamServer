(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [com.sixsq.slipstream.util.response :as ru])
  (:import
    (com.amazonaws HttpMethod)
    (com.amazonaws.auth AWSStaticCredentialsProvider BasicAWSCredentials)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model CreateBucketRequest DeleteObjectRequest
                                     GeneratePresignedUrlRequest ResponseHeaderOverrides)))


(def ^:const default-ttl 15)

(def request-admin {:identity     {:current "internal"
                                   :authentications
                                            {"internal" {:roles #{"ADMIN"}, :identity "internal"}}}
                    :sixsq.slipstream.authn/claims
                                  {:username "internal", :roles "ADMIN"}
                    :params       {:resource-name "user"}
                    :route-params {:resource-name "user"}
                    :user-roles   #{"ANON"}})

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

(defn delete-s3-object [obj-store-conf bucket obj-name]
  (let [deleteRequest (DeleteObjectRequest. bucket obj-name)]
    (.deleteObject (get-s3-client obj-store-conf) deleteRequest)))

(defn bucket-exists?
  "Function mocked in unit tests"
  [s3client bucketName]
  (.doesBucketExist s3client bucketName))

(defn create-bucket!
  "Caller should have checked that bucket does not exist yet.
  If creation fails, an Exception is thrown
  Mocked in unit tests"
  [s3client bucket-name]
  (.createBucket s3client (CreateBucketRequest. bucket-name)))




;;Pre-conditions for adding external-object

(defn cannot-create-bucket?
  "When the requested bucket doesn't exist and can't be created.
  The external object resource must not be created."
  [{:keys [bucketName] :as resource} s3client]
  (if (bucket-exists? s3client bucketName)
    false
    (try
      (create-bucket! s3client bucketName)
      false
      (catch Exception e
        (log/error (format "Error when creating bucket %s: %s" bucketName (.getMessage e)))
        true))))

(defn cond2?
  "When the requested bucket exists, but the user doesn't have access to it. The external object resource must not be created."
  [resource request]
  false
  )
(defn cond3?
  "When the bucket exists, but the user can't create the object. The external object resource must not be created."
  [resource request]
  false
  )


;;Pre-conditions for deleting eo
(defn cond4?
  "When the user requests to delete an object, but it no longer exists. The external object resource should be deleted normally."
  [resource request]
  false
  )
(defn cond5?
  "When the user cannot access the bucket/object to delete it. The external object resource should not be deleted."
  [resource request]
  false
  )


(defn expand-cred
  "Returns credential document after expanding `href-obj-store-cred` credential href.

  Deriving objectType from request is not directly possible as this is a request on
  action resource. We would need to get resource id, load the resource and get
  objectType from it. Instead, requiring objectType as parameter. It should be known
  to the callers."
  [href-obj-store-cred]
  (std-crud/resolve-hrefs href-obj-store-cred request-admin true))

(defn expand-obj-store-creds
  "Need objectType to dispatch on when loading credentials."
  [href-obj-store-cred request objectType]
  (let [{:keys [key secret connector]} (expand-cred href-obj-store-cred)]
    {:key      key
     :secret   secret
     :endpoint (:objectStoreEndpoint connector)}))

(defn ok-to-add-external-resource?
  "Determines if S3 conditions are met on S3 for the user to safely
  add an external object resource. If everything is OK, then the resource
  itself is returned. Otherwise an 'unauthorized' response map is thrown"
  [{:keys [objectStoreCred objectType] :as resource} request]
  (let [
        obj-store-conf (expand-obj-store-creds objectStoreCred request objectType)
        s3client (get-s3-client obj-store-conf)]
    (cond
      (cannot-create-bucket? resource s3client) (logu/log-and-throw 503 (format "Unable to create the bucket %s" (:bucketName resource)))
      (cond2? resource request) (throw (ru/ex-unauthorized (:resource-id resource)))
      (cond3? resource request) (throw (ru/ex-unauthorized (:resource-id resource)))
      :all-ok resource
      )))



