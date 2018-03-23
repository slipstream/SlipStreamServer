(ns com.sixsq.slipstream.ssclj.persistence.zookeeper-binding
  (:require [clojure.tools.logging :as log]
            [com.sixsq.slipstream.ssclj.util.zookeeper :as zku]))

(defn initialize
  []
  (try
    (zku/set-client! (zku/create-client))
    (catch Exception e
      (log/warn "error creating zookeeper client:" (str e)))))

(defn finalize
  []
  (try
    (zku/close-client!)
    (log/info "zookeeper client closed")
    (catch Exception e
      (log/warn "zookeeper client close failed:" (str e)))))
