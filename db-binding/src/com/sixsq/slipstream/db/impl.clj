(ns com.sixsq.slipstream.db.impl
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.db.binding :as p]))

(def ^:dynamic *impl*)

(defn set-impl!
  [impl]
  (alter-var-root #'*impl* (constantly impl)))

(defn unset-impl!
  []
  (.unbindRoot #'*impl*))

(defn initialize [collection-id options]
  (log/debug "DB impl initialize" collection-id " with options " options)
  (p/initialize *impl* collection-id options))

(defn add [collection-id data options]
  (p/add *impl* collection-id data options))

(defn retrieve [id options]
  (p/retrieve *impl* id options))

(defn edit [data options]
  (p/edit *impl* data options))

(defn delete [data options]
  (p/delete *impl* data options))

(defn query [collection-id options]
  (p/query *impl* collection-id options))
