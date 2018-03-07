(ns com.sixsq.slipstream.ssclj.app.persistent-db
  (:require
    [com.sixsq.slipstream.db.binding :as p]
    [com.sixsq.slipstream.db.impl :as orig-impl]))

(defn set-impl!
  [impl]
  (alter-var-root #'orig-impl/*impl* (constantly impl)))

(defn unset-impl!
  []
  (.unbindRoot #'orig-impl/*impl*))

(defn add [collection-id data options]
  (p/add orig-impl/*impl* collection-id data options))

(defn retrieve [id options]
  (p/retrieve orig-impl/*impl* id options))

(defn edit [data options]
  (p/edit orig-impl/*impl* data options))

(defn delete [data options]
  (p/delete orig-impl/*impl* data options))

(defn query [collection-id options]
  (p/query orig-impl/*impl* collection-id options))
