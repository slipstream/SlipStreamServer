(ns com.sixsq.slipstream.db.es.javawrapper
  (:require
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.impl :as db])
  (:gen-class
    :name com.sixsq.slipstream.ssclj.es.JavaWrapper
    :methods [#^{:static true} [createInmemEsDb [] void]]))

(defn -createInmemEsDb
  []
  (esb/set-client! (esb/create-test-client))
  (db/set-impl! (esb/get-instance))
  (esu/reset-index esb/*client* esb/index-name))
