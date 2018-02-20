(ns com.sixsq.slipstream.db.atom.binding-test
  (:require
    [clojure.test :refer [deftest is are]]
    [duratom.core :as duratom]
    [com.sixsq.slipstream.db.binding-lifecycle :as lifecycle]
    [com.sixsq.slipstream.db.atom.binding :as t]))

(deftest check-standard-atom
  (lifecycle/check-binding-lifecycle (t/->AtomBinding (atom {}))))

(deftest check-duratom
  (lifecycle/check-binding-lifecycle (t/->AtomBinding (duratom/duratom :local-file
                                                                       :file-path "target/duratom-db"
                                                                       :init {}))))
