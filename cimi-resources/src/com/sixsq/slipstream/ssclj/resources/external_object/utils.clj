(ns com.sixsq.slipstream.ssclj.resources.external-object.utils
  (:use

    [amazonica.core]
    [amazonica.aws.s3 :as s3])
  (:require [environ.core :as env]
            [clj-time.core :as t]
            [clj-http.client :as client]))

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
   (let [expiry (-> mn t/minutes t/from-now)]
     (.toString
       (if write?
         (generate-presigned-url (get-s3-cred) bucket k expiry "PUT")
         (generate-presigned-url (get-s3-cred) bucket k expiry))))))



