(ns com.sixsq.slipstream.ssclj.resources.spec.accounting-record-obj
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.accounting-record :as acc]))


(s/def :cimi.accounting-record-obj/size pos-int?)

(def accounting-keys-obj-specs
  {
   :req-un [:cimi.accounting-record-obj/size]
   }
  )

(s/def :cimi/accounting-record.obj
  (su/only-keys-maps acc/accounting-record-keys-spec
                     accounting-keys-obj-specs))

