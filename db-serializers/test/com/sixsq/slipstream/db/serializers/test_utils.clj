(ns com.sixsq.slipstream.db.serializers.test-utils
  (:require
    [com.sixsq.slipstream.db.serializers.utils :as u]))

;; Fixtures.
(defn fixture-start-es-db
  [f]
  (u/with-test-es-client-and-db-impl
    (f)))

