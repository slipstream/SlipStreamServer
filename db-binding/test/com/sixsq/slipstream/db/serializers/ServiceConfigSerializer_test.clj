(ns com.sixsq.slipstream.db.serializers.ServiceConfigSerializer-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.serializers.ServiceConfigSerializer :as scs])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter]
    ))

(defn create-es-client
  []
  (db/set-impl! (esb/get-instance))
  (esb/set-client! (esb/create-test-client)))

(create-es-client)

(deftest test-save
  (let [sc (ServiceConfiguration.)
        _ (.setParameter sc (ServiceConfigurationParameter. "support-email" "support@sixsq.com" "Support email."))
        new-sc (scs/-store sc)
        _ (scs/-load)]
    (is (= new-sc sc))))
