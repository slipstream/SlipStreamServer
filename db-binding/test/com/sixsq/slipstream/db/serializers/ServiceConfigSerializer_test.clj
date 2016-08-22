(ns com.sixsq.slipstream.db.serializers.ServiceConfigSerializer-test
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [schema.core :as sch]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.db.serializers.test-utils :as tu]
    [com.sixsq.slipstream.db.serializers.ServiceConfigSerializer :refer :all])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    [com.sixsq.slipstream.persistence ParameterType]
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter]
    ))

#_(deftest test-sc-to-resource
    )

(def sc-from-xml (tu/conf-xml->sc))

(defn- keys-not-in-conf
  [conf]
  (for [k (keys rname->param) :when (not (contains? conf k))]
    k))

(def valid-acl {:owner {:principal "me" :type "USER"}})

(def conf-extra
  {:id          (str cr/resource-name "/slipstream")
   :resourceURI cr/resource-uri
   :created     "1970-01-01T00:00:00.0Z"
   :updated     "1970-01-01T00:00:00.0Z"
   :acl         valid-acl})

(deftest test-sc->configuration-resource
  (is (= {} (sc->configuration-resource (ServiceConfiguration.))))
  (let [conf (sc->configuration-resource sc-from-xml)
        not-in-conf (keys-not-in-conf conf)]
    (is (empty? not-in-conf) (str "The following keys are not in config " (clojure.string/join ", " not-in-conf)))
    ))

(deftest test-check-sc-schema
  (is (nil? (sch/check cr/Configuration (merge conf-extra (sc->configuration-resource sc-from-xml)))))
  )

#_(deftest test-save-load
    (let [to-es (-store (tu/conf-xml->sc))
          from-es (-load)]
      (is (= (.getId to-es) (.getId from-es)))))
