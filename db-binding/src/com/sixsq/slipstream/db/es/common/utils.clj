(ns com.sixsq.slipstream.db.es.common.utils
  (:require
    [com.sixsq.slipstream.db.utils.common :as cu]
    [camel-snake-kebab.core :refer [->kebab-case]]))


(def default-index-prefix "slipstream-")


(defn id->index
  ([id]
    (id->index default-index-prefix id))
  ([index-prefix id]
   (->> id cu/split-id first (str index-prefix))))


(defn collection-id->index
  ([collection-id]
   (collection-id->index default-index-prefix collection-id))
  ([index-prefix collection-id]
   (str index-prefix (->kebab-case collection-id))))
