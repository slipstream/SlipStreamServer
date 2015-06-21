(ns com.sixsq.slipstream.ssclj.db.filesystem-binding-utils
  (:require
    [fs.core                                :as fs]
    [clojure.data.json                      :as json]
    [com.sixsq.slipstream.ssclj.db.binding  :refer [Binding]]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn serialize
  [resource]
  (with-out-str
    (json/pprint resource :key-fn name)))

(defn serialize-file
  [f resource]
  (->> f
       (fs/parent)
       (fs/mkdirs))
  (->> resource
       (serialize)
       (spit f))
  true)

(defn deserialize
  [s]
  (json/read-str s :key-fn keyword))

(defn deserialize-file
  [f]
  (->> f
       (slurp)
       (deserialize)))

