(ns com.sixsq.slipstream.ssclj.resources.spec.module
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(def ^:const path-regex #"^[a-zA-Z][\w\.-]*(/[a-zA-Z][\w\.-]*)*$")

(defn path? [v] (re-matches path-regex v))

(defn parent-path? [v] (or (= "" v) (re-matches path-regex v)))

(s/def ::path (s/and string? path?))

(s/def ::parentPath (s/and string? parent-path?))

(s/def ::type #{"PROJECT" "IMAGE" "COMPONENT" "APPLICATION"})

(s/def ::versions (s/coll-of (s/nilable ::cimi-common/resource-link) :min-count 1))

(s/def ::logo ::cimi-common/resource-link)

(def module-keys-spec (su/merge-keys-specs [c/common-attrs
                                                      {:req-un [::path ::parentPath ::type]
                                                       :opt-un [::logo ::versions]}]))

(s/def ::module (su/only-keys-maps module-keys-spec))
