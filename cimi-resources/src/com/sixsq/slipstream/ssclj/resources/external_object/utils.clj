(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:require
    [amazonica.core :as aws]
    [amazonica.aws.s3 :as s3]
    [clj-time.core :as time]))

(defn get-s3-cred
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
                  (.split "\n"))]
    (merge
      (clojure.set/rename-keys
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
      config)))


(defn generate-url
  ([bucket k mn]
   (generate-url bucket k mn false))
  ([bucket k mn write?]
   (let [expiry (-> mn time/minutes time/from-now)
         method (if write? "PUT" "GET")]
     (.toString
       (s3/generate-presigned-url (get-s3-cred) bucket k expiry method)))))

(defn delete-s3-object
  [bucket k]
  (let [cred (get-s3-cred)]
    (aws/with-credential [(:access-key cred)
                          (:secret-key cred)
                          (:endpoint cred)]
                         (when (s3/does-object-exist cred bucket k)
                           (s3/delete-object bucket k)))))





