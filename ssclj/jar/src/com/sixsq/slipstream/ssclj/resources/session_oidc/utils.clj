(ns com.sixsq.slipstream.ssclj.resources.session-oidc.utils
  (:require [ring.util.codec :as codec]
            [com.sixsq.slipstream.ssclj.resources.session :as p]
            [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
            [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
            [com.sixsq.slipstream.auth.utils.http :as uh]
            [clojure.tools.logging :as log]
            [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
            [com.sixsq.slipstream.ssclj.util.response :as r]
            [clojure.string :as str]))

(defn prefix
  [realm attr]
  (when (and realm attr)
    (str realm ":" attr)))

(defn extract-roles
  [{:keys [realm roles] :as claims}]
  (if (and realm roles)
    (vec (map (partial prefix realm) roles))
    []))

(defn group-hierarchy
  [group]
  (if-not (str/blank? group)
    (let [terms (remove str/blank? (str/split group #"/"))]
      (doall
        (for [i (range 1 (+ 1 (count terms)))]
          (str "/" (str/join "/" (take i terms))))))
    []))

(defn extract-groups
  [{:keys [realm groups] :as claims}]
  (if (and realm groups)
    (->> groups
         (mapcat group-hierarchy)
         (map (partial prefix realm))
         vec)
    []))
