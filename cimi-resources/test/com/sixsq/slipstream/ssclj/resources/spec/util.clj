(ns com.sixsq.slipstream.ssclj.resources.spec.util
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [expound.alpha :refer [expound-str]]))

(defn is-valid
  [spec resource]
  (is (s/valid? spec resource) (expound-str spec resource)))

(defn is-not-valid
  [spec resource]
  (is (not (s/valid? spec resource)) (expound-str spec resource)))

(defn print-spec
  [spec]
  (clojure.pprint/pprint (s/form spec)))

