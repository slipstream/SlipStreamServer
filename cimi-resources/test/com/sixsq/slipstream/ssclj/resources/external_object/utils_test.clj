(ns com.sixsq.slipstream.ssclj.resources.external-object.utils-test
  (:require
    [clojure.test :refer :all]
    [clojure.string :as s]
    [com.sixsq.slipstream.ssclj.resources.external-object.utils :as u]))


(deftest test-generate-url
  (let [os-host "s3.cloud.com"
        obj-store-conf {:endpoint (str "https://" os-host)
                        :key      "key"
                        :secret   "secret"}
        bucket "bucket-name"
        obj-name "object/name"
        verb :put]
    (is (s/starts-with? (u/generate-url obj-store-conf bucket obj-name verb)
                        (format "https://%s.%s/%s?" bucket os-host obj-name)))))
