(ns com.sixsq.slipstream.auth.utils.timestamp-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [clojure.string :as str]
    [com.sixsq.slipstream.auth.utils.timestamp :as t]))

(deftest check-expiry-timestamp
  (is (pos? (t/expiry-later)))
  (is (pos? (t/expiry-later 10)))
  (is (< (t/expiry-later 10) (t/expiry-later 20)))
  (is (< (t/expiry-now) (t/expiry-later))))

(deftest check-timestamp-formatting
  (is (not (str/blank? (t/formatted-expiry-later))))
  (is (re-matches #".*UTC$" (t/formatted-expiry-later)))
  (is (not (str/blank? (t/formatted-expiry-later 10))))
  (is (re-matches #".*UTC$" (t/formatted-expiry-later 10))))

(deftest check-expiry-now
  (is (not (str/blank? (t/formatted-expiry-now))))
  (is (re-matches #".*UTC$" (t/formatted-expiry-now))))
