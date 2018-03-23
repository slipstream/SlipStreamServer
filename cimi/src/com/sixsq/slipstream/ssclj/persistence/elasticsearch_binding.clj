(ns com.sixsq.slipstream.ssclj.persistence.elasticsearch-binding
  (:require [clojure.tools.logging :as log]
            [com.sixsq.slipstream.db.impl :as db]
            [com.sixsq.slipstream.db.es.binding :as esb]))


(defn- set-persistence-impl
  []
  (db/set-impl! (esb/get-instance)))

(defn initialize
  []
  (try
    (esb/set-client! (esb/create-client))
    (catch Exception e
      (log/warn "error creating elasticsearch client:" (str e))))

  (try
    (set-persistence-impl)
    (catch Exception e
      (log/warn "error setting persistence implementation:" (str e)))))

(defn finalize
  []
  (try
    (esb/close-client!)
    (log/info "elasticsearch client closed")
    (catch Exception e
      (log/warn "elasticsearch client close failed:" (str e)))))