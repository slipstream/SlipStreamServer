(ns com.sixsq.slipstream.ssclj.db.impl
  (:require
    [com.sixsq.slipstream.ssclj.db.binding :as p]
    [clojure.tools.logging :as log]))

(def ^:dynamic *impl*)

(defn set-impl!
  [impl]
  (alter-var-root #'*impl* (constantly impl)))

(defn add [collection-id data]
  (p/add *impl* collection-id data))

(defn retrieve [id]
  (p/retrieve *impl* id))

(defn edit [data]
  (p/edit *impl* data))

(defn delete [data]
  (p/delete *impl* data))

(defn query [collection-id options]
  (p/query *impl* collection-id options))

