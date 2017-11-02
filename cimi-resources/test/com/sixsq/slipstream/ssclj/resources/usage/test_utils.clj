(ns com.sixsq.slipstream.ssclj.resources.usage.test-utils
    (:require
      [clj-time.core :as time]
      [com.sixsq.slipstream.ssclj.resources.usage.utils :as u]))

(def not-before? (complement time/before?))

(defn ordered-desc?
        [timestamps]
        (every? (fn [[a b]] (not-before? (u/to-time a) (u/to-time b))) (partition 2 1 timestamps)))

(defn submap?
        "True if a is a submap of b
        (submap? {:a 1 :b 2} {:a 1 :b 2 :c 3})  => true
        (submap? {:a 1 :b 2} {:a 8 :b 2 :c 3})  => false
        (submap? {:a 1 :b 2} {:a 1 :c 3})       => false
        "
        [a b]
        (every? (fn [[k v]] (= (b k) v)) a))

