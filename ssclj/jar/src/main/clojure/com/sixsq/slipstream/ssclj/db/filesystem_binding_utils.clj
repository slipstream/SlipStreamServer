(ns com.sixsq.slipstream.ssclj.db.filesystem-binding-utils
  (:require
    [fs.core :as fs]
    [clojure.data.json :as json]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.db.binding :refer [Binding]]
    [ring.util.response :as r]))

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

