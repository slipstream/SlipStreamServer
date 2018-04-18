(ns com.sixsq.slipstream.ssclj.resources.spec.util
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [expound.alpha :refer [expound-str]]))

(defmacro spec-valid?
  [spec resource]
  `((fn [spec# resource#]
      (is (s/valid? spec# resource#) (expound-str spec# resource#)))
     ~spec ~resource))

(defmacro spec-not-valid?
  [spec resource]
  `((fn [spec# resource#]
      (is (not (s/valid? spec# resource#)) (expound-str spec# resource#)))
     ~spec ~resource))

(defn print-spec
  [spec]
  (clojure.pprint/pprint (s/form spec)))

