(ns com.sixsq.slipstream.run.quota-setup
  (:require
    [clojure.java.io :as io]
    [com.sixsq.slipstream.db.serializers.utils :as dbu]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.serializers.service-config-util :as scu]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]))

(defn init-config []
  (esb/set-client! (esb/create-test-client))
  (dbu/set-db-crud-impl)
  (sci/db-add-default-config))
