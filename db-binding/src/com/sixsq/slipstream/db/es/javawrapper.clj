(ns com.sixsq.slipstream.db.es.javawrapper
  (:require
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.impl :as db])
  (:gen-class
    :name com.sixsq.slipstream.db.es.JavaWrapper
    :methods [#^{:static true} [createInmemEsDb [] void]]))

(defn -createInmemEsDb
  []
  (let [[client node] (esb/create-client)]
    (esb/set-client! client)                                ;; NOTE: client and node are not closed!
    (db/set-impl! (esb/get-instance))
    (esu/reset-index esb/*client* esb/index-name)))
