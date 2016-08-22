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

(def sc-from-xml (tu/conf-xml->sc))

(defn- keys-not-in-conf
  [conf]
  (for [k (keys rname->param) :when (not (contains? conf k))]
    k))

(defn msg-keys-not-in-cfg
  [keys]
  (str "The following keys are not in config " (clojure.string/join ", " keys)))

(def conf-extra
  {:id          (str cr/resource-name "/slipstream")
   :resourceURI cr/resource-uri
   :created     "1970-01-01T00:00:00.0Z"
   :updated     "1970-01-01T00:00:00.0Z"
   :acl         valid-acl})

(defn sc-get-param-value
  [sc k]
  (if-let [p (.getParameter sc k)]
    (.getValue p)
    ""))

;; Fixtures
(use-fixtures :once tu/fixture-start-es-db)

;; Tests
(deftest test-sc->configuration-resource
  (is (= {} (sc->cfg (ServiceConfiguration.))))
  (let [conf (sc->cfg sc-from-xml)
        not-in-conf (keys-not-in-conf conf)]
    (is (empty? not-in-conf) (msg-keys-not-in-cfg not-in-conf))))

(deftest test-check-sc-schema
  (is (nil? (sch/check cr/Configuration (merge conf-extra (sc->cfg sc-from-xml))))))

(deftest test-save-load
    (let [sc-to-es (-store sc-from-xml)
          sc-from-es (-load)]
      (is (not (nil? sc-from-es)))
      (is (= (sc-get-param-value sc-to-es "slipstream.registration.email")
             (sc-get-param-value sc-from-es "slipstream.registration.email")))
      (is (= (sc-get-param-value sc-to-es "slipstream.mail.ssl")
             (sc-get-param-value sc-from-es "slipstream.mail.ssl")))
      (is (= (sc-get-param-value sc-to-es "slipstream.mail.port")
             (sc-get-param-value sc-from-es "slipstream.mail.port")))))
