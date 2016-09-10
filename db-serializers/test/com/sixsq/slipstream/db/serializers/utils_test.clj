(ns com.sixsq.slipstream.db.serializers.utils-test
  (:require
    [clojure.string :as s]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.db.serializers.utils :as u])
  (:import
    (com.sixsq.slipstream.persistence ServiceConfigurationParameter)))

(def scp-enum-instr (let [p (ServiceConfigurationParameter. "foo" "bar" "baz")]
                      (.setEnumValues p ["u" "f" "o"])
                      (.setInstructions p "Dolorem ipsum.")
                      p))

(def scp-instr (let [p (ServiceConfigurationParameter. "foo" "bar" "baz")]
                 (.setEnumValues p nil)
                 (.setInstructions p "Dolorem ipsum.")
                 p))

(deftest test-desc-from-param-with-instructions-and-enum
  (let [pd (u/desc-from-param scp-enum-instr)]
    (is (contains? pd :instructions))
    (is (= "Dolorem ipsum." (:instructions pd)))
    (is (contains? pd :enum))
    (is (= 3 (count (:enum pd))))
    (is (= "ufo" (s/join (:enum pd))))
    ))

(deftest test-desc-from-param-with-instructions-no-enum
  (let [pd (u/desc-from-param scp-instr)]
    (is (contains? pd :instructions))
    (is (= "Dolorem ipsum." (:instructions pd)))
    ))
