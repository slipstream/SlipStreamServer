(ns com.sixsq.slipstream.db.serializers.service-config
  (:refer-clojure :exclude [load])
  (:require
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]))

;;
;; Interface to store and load entity as resource.
;;

(defn store
  "Stores ServiceConfiguration per connector parameters.
  Returns provided ServiceConfiguration."
  [^Object sc]
  (-> sc
      sci/store-connectors))

(defn load
  "Loads and returns ServiceConfiguration with global and per
  connector parameters."
  [^Object sc]
  (-> sc
      sci/load-sc
      sci/load-connectors))
