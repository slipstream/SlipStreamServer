(ns com.sixsq.slipstream.ssclj.resources.spec.acl
  "Access Control Lists (an extension to the CIMI standard)."
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::principal ::cimi-core/nonblank-string)

(s/def ::type #{"USER" "ROLE"})
(s/def ::right #{"VIEW" "MODIFY"                            ;; LEGACY RIGHTS
                 "ALL"
                 "VIEW_ACL" "VIEW_DATA" "VIEW_META"
                 "EDIT_ACL" "EDIT_DATA" "EDIT_META"
                 "DELETE"
                 "MANAGE"})

(s/def ::owner (su/only-keys :req-un [::principal
                                      ::type]))

(s/def ::rule (su/only-keys :req-un [::principal
                                     ::type
                                     ::right]))
(s/def ::rules (s/coll-of ::rule :min-count 1))


