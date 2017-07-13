(ns com.sixsq.slipstream.ssclj.resources.spec.accounting-record-vm
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.accounting-record :as acc]))


(s/def :cimi.accounting-record-vm/cpu pos-int?)
(s/def :cimi.accounting-record-vm/ram pos-int?)
(s/def :cimi.accounting-record-vm/disk pos-int?)
(s/def :cimi.accounting-record-vm/instanceType :cimi.core/nonblank-string)

(def accounting-keys-vm-specs
  {:req-un [:cimi.accounting-record-vm/cpu
            :cimi.accounting-record-vm/ram
            :cimi.accounting-record-vm/instanceType
            ]
   :opt-un [:cimi.accounting-record-vm/disk]})

(s/def :cimi/accounting-record.vm
  (su/only-keys-maps acc/accounting-record-keys-spec
                     accounting-keys-vm-specs))

