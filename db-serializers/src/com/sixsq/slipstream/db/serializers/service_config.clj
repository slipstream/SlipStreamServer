(ns com.sixsq.slipstream.db.serializers.service-config
  (:require
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.db.serializers.utils :as u]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]))

;;
;; Interface to store and load entity as resource.
;;

(defn store
  [^ServiceConfiguration sc]
  (-> sc
      sci/sc->cfg
      sci/as-request
      cr/edit-impl
      u/throw-on-resp-error)
  sc)

(defn load
  []
  (let [cfg (-> (sci/as-request)
                cr/retrieve-impl
                :body)
        cfg-desc (sci/load-cfg-desc)]
    (sci/cfg->sc cfg cfg-desc)))

