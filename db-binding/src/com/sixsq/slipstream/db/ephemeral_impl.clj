(ns com.sixsq.slipstream.db.ephemeral-impl
  (:require
    [com.sixsq.slipstream.db.binding :as p])
  (:import
    (java.io Closeable)))

(def ^:dynamic *impl*)


(defn set-impl!
  [impl]
  (alter-var-root #'*impl* (constantly impl)))


(defn unset-impl!
  []
  (.unbindRoot #'*impl*))


(defn initialize [collection-id options]
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


(defn close []
  (when-let [^Closeable impl *impl*]
    (try
      (unset-impl!)
      (.close impl)
      (catch Exception _))))
