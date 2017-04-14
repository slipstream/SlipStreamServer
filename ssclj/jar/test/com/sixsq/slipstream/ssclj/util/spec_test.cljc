(ns com.sixsq.slipstream.ssclj.util.spec-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec :as s]
    [com.sixsq.slipstream.ssclj.util.spec :as t]))

(deftest check-unnamespaced-kws
  (is (= #{} (t/unnamespaced-kws nil)))
  (is (= #{} (t/unnamespaced-kws [])))
  (is (= #{:a :b :c} (t/unnamespaced-kws [:a :alpha/b :alpha.beta/c])))
  (is (thrown? Exception (t/unnamespaced-kws [:a 3]))))

(deftest check-allowed-keys
  (let [keys-spec {:req     [:ns1/one :ns2/two]
                   :req-un  [:ns3/three]
                   :opt     [:ns4/four]
                   :opt-un  [:ns5/five :ns6/six]
                   :ignored [:ns7/seven]}]
    (is (= #{:ns1/one :ns2/two :three :ns4/four :five :six} (t/allowed-keys keys-spec)))))

(s/def :ns1/one boolean?)
(s/def :ns2/two string?)
(s/def :ns3/three keyword?)
(s/def :ns4/four pos-int?)
(s/def :spec.test/closed (t/only-keys :req [:ns1/one]
                                      :req-un [:ns2/two]
                                      :opt [:ns3/three]
                                      :opt-un [:ns4/four]))

(def keys-spec-req {:req [:ns1/one], :req-un [:ns2/two]})
(s/def :spec.test/also-closed (t/only-keys-maps keys-spec-req {:opt [:ns3/three], :opt-un [:ns4/four]}))

(deftest check-only-keys
  (let [valid-map {:ns1/one   true
                   :two       "OK"
                   :ns3/three :ok
                   :four      42}]
    (are [expect-fn arg] (expect-fn (s/valid? :spec.test/closed arg))
                         true? valid-map
                         false? (dissoc valid-map :ns1/one)
                         false? (dissoc valid-map :two)
                         true? (dissoc valid-map :ns3/three)
                         true? (dissoc valid-map :four)
                         false? (assoc valid-map :bad "BAD"))
    (are [expect-fn arg] (expect-fn (s/valid? :spec.test/also-closed arg))
                         true? valid-map
                         false? (dissoc valid-map :ns1/one)
                         false? (dissoc valid-map :two)
                         true? (dissoc valid-map :ns3/three)
                         true? (dissoc valid-map :four)
                         false? (assoc valid-map :bad "BAD"))))
