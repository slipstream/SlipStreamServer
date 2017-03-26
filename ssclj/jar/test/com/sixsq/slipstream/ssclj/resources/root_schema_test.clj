(ns com.sixsq.slipstream.ssclj.resources.root-schema-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.root :refer :all]
    [schema.core :as s]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(defn valid? [root] (nil? (s/check Root root)))
(def invalid? (complement valid?))

(deftest check-root-schema
         (let [timestamp "1964-08-25T10:00:00.0Z"
               root {:id          resource-name
                     :resourceURI p/service-context
                     :created     timestamp
                     :updated     timestamp
                     :acl         resource-acl
                     :baseURI     "http://cloud.example.org/"}]

           (is (valid? root))
           (is (valid? (assoc root :resources {:href "Resource/uuid"})))
           (is (invalid? (dissoc root :created)))
           (is (invalid? (dissoc root :updated)))
           (is (invalid? (dissoc root :baseURI)))
           (is (invalid? (dissoc root :acl)))))
