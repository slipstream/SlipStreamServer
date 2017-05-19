(ns com.sixsq.slipstream.ssclj.resources.spec.description
  "Spec definitions for the descriptions of resources. These descriptions
   provide information to clients to allow them to understand the contents
   of resources."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))

;;
;; parameter descriptions for providing help information to clients
;; these definitions are the 'cimi.desc' namespace
;;

(s/def :cimi.desc/displayName :cimi.core/nonblank-string)
(s/def :cimi.desc/category :cimi.core/nonblank-string)
(s/def :cimi.desc/description :cimi.core/nonblank-string)
(s/def :cimi.desc/type #{"string" "boolean" "int" "float" "timestamp" "enum" "map" "list"})
(s/def :cimi.desc/mandatory boolean?)
(s/def :cimi.desc/readOnly boolean?)
(s/def :cimi.desc/order nat-int?)
(s/def :cimi.desc/enum (s/coll-of :cimi.core/nonblank-string :min-count 1))
(s/def :cimi.desc/instructions :cimi.core/nonblank-string)

(s/def :cimi.desc/parameter-description
  (su/only-keys :req-un [:cimi.desc/type]
                :opt-un [:cimi.desc/displayName
                         :cimi.desc/category
                         :cimi.desc/description
                         :cimi.desc/mandatory
                         :cimi.desc/readOnly
                         :cimi.desc/order
                         :cimi.desc/enum
                         :cimi.desc/instructions]))

(s/def :cimi.desc/resource-description
  (s/every (s/or :acl (s/tuple #{:acl} :cimi.common/acl)
                 :desc (s/tuple keyword? :cimi.desc/parameter-description))))

