(ns com.sixsq.slipstream.ssclj.db.filesystem-binding-utils
  (:require
    [fs.core                                :as fs]
    [clojure.data.json                      :as json]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as cu]
    [com.sixsq.slipstream.ssclj.db.binding  :refer [Binding]]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))


(defn serialize-file
  [f resource]
  (->> f
       (fs/parent)
       (fs/mkdirs))
  (->> resource
       (cu/serialize)
       (spit f))
  true)

(defn deserialize-file
  [f]
  (->> f
       (slurp)
       (cu/deserialize)))

