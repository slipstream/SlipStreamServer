(ns com.sixsq.slipstream.db.serializers.service-config
  (:require
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci])
  (:import
    (com.sixsq.slipstream.persistence ServiceConfiguration)))

;;
;; Interface to store and load entity as resource.
;;

(defn store
  "Stores ServiceConfiguration global and per connector parameters.
  Returns provided ServiceConfiguration."
  [^ServiceConfiguration sc]
  (-> sc
      sci/store-sc
      sci/store-connectors))

(defn load
  "Loads and returns ServiceConfiguration with global and per
  connector parameters."
  []
  (-> (sci/load-sc)
      sci/load-connectors))
