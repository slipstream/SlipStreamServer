(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [clj-time.coerce :as tc]
    [clj-time.core :as t]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.util.log :as logu]
    [sixsq.slipstream.client.impl.utils.http-sync :as http])
  (:import
    (com.amazonaws HttpMethod)
    (com.amazonaws.auth AWSStaticCredentialsProvider BasicAWSCredentials)
    (com.amazonaws.client.builder AwsClientBuilder$EndpointConfiguration)
    (com.amazonaws.services.s3 AmazonS3ClientBuilder)
    (com.amazonaws.services.s3.model CreateBucketRequest DeleteObjectRequest
                                     GeneratePresignedUrlRequest)))


(def ^:const default-ttl 15)


(def request-admin {:identity                      {:current         "internal"
                                                    :authentications {"internal" {:roles    #{"ADMIN"}
                                                                                  :identity "internal"}}}
                    :sixsq.slipstream.authn/claims {:username "internal"
                                                    :roles    "ADMIN"}
                    :params                        {:resource-name "user"}
                    :route-params                  {:resource-name "user"}
                    :user-roles                    #{"ANON"}})

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
  "Caller should have checked that bucket does not exist yet. If creation
  fails, an Exception is thrown; otherwise true is returned. Mocked in unit
  tests."
  [s3client bucket-name]
  (.createBucket s3client (CreateBucketRequest. bucket-name))
  true)


(defn expand-cred
  "Returns credential document after expanding `href-obj-store-cred` credential href.

  Deriving objectType from request is not directly possible as this is a request on
  action resource. We would need to get resource id, load the resource and get
  objectType from it. Instead, requiring objectType as parameter. It should be known
  to the callers."
  [href-obj-store-cred]
  (std-crud/resolve-hrefs href-obj-store-cred request-admin true))

;;Pre-conditions for adding external-object


(defn bucket-creation-ok?
  "When the requested bucket doesn't exist and can't be created.
  The external object resource must not be created."
  [s3client bucketName]
  (try
    (or (bucket-exists? s3client bucketName)
        (create-bucket! s3client bucketName))
    (catch Exception e
      (log/error (format "Error when creating bucket %s: %s" bucketName (.getMessage e)))
      false)))


(defn authorized-bucket-operation?
  [{:keys [objectStoreCred] :as resource} request check-authn-fn]
  (-> objectStoreCred
      (expand-cred)
      (check-authn-fn request)))


(defn uploadable-bucket?
  "When the bucket exists, but the user can't create the object.
  The external object resource must not be created."
  [obj-store-conf bucketName]
  (let [upload-url (generate-url obj-store-conf bucketName "test-probe" :put {:ttl 3 :content-type "text/plain" :filename "test.txt"})]
    (= 200 (http/put upload-url))))


(defn expand-obj-store-creds
  "Need objectType to dispatch on when loading credentials."
  [href-obj-store-cred]
  (let [{:keys [key secret connector]} (expand-cred href-obj-store-cred)]
    {:key      key
     :secret   secret
     :endpoint (:objectStoreEndpoint connector)}))


(defn ok-to-add-external-resource?
  "Determines if S3 conditions are met on S3 for the user to safely
  add an external object resource. If everything is OK, then the resource
  itself is returned. Otherwise an error response map is thrown"
  [{:keys [bucketName objectStoreCred] :as resource} request]
  (let [
        obj-store-conf (expand-obj-store-creds objectStoreCred)
        s3client (get-s3-client obj-store-conf)]
    (cond
      (not (bucket-creation-ok? s3client bucketName)) (logu/log-and-throw 503 (format "Unable to create the bucket %s" bucketName))
      ;; When the requested bucket exists, but the user doesn't have permission to it :
      ;; The external object resource must not be created."
      (not (authorized-bucket-operation? resource request a/can-view?)) (logu/log-and-throw 403 (format "Access to bucket %s is not allowed" bucketName))
      (not (uploadable-bucket? obj-store-conf bucketName)) (logu/log-and-throw 503 (format "Unable to create objects in the bucket %s" bucketName))
      :all-ok resource)))


(defn ok-to-delete-external-resource?
  "Determines if S3 conditions are met on S3 for the user to safely
  delete an external object resource. If everything is OK, then the resource
  itself is returned. Otherwise an 'unauthorized' response map is thrown"
  [{:keys [bucketName objectStoreCred] :as resource} request]
  (let [
        obj-store-conf (expand-obj-store-creds objectStoreCred)
        s3client (get-s3-client obj-store-conf)]
    (cond
      ;; When the user requests to delete an object, but it no longer exists :
      ;; The external object resource should be deleted normally.
      (not (bucket-exists? s3client bucketName)) resource
      ;; "When the user cannot access the bucket/object to delete it :
      ;; the external object resource should not be deleted."
      (not (authorized-bucket-operation? resource request a/can-modify?)) (logu/log-and-throw 403 (format "Deleting the bucket %s is not allowed" bucketName))
      :all-ok resource)))



