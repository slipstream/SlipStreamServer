(ns com.sixsq.slipstream.ssclj.resources.spec.schema.spec-tools-live-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.dbtest.es.spandex-utils :as spu]
            [com.sixsq.slipstream.ssclj.resources.spec.schema.spec-tools :as t]
            [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
            [com.sixsq.slipstream.ssclj.resources.spec.schema.util :as scu])
  (:import (java.util UUID))
  )



(deftest create-mappings
  (let [index-empty (str (UUID/randomUUID))
        index-metering (str (UUID/randomUUID))
        _ (spu/provide-mock-rest-client)
        ]

    (is (= 200 (-> (scu/create-index index-empty {})
                   :status)))

    (is (= 200 (-> (scu/create-index index-metering (t/spec->es-mapping :cimi/metering))
                   :status)))

    (scu/remove-index index-empty)
    (scu/remove-index index-metering)

    )

  )