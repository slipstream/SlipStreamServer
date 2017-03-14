(ns com.sixsq.slipstream.auth.cookies-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [clj-time.coerce :as c]
    [com.sixsq.slipstream.auth.env-fixture :as env-fixture]
    [com.sixsq.slipstream.auth.cookies :as t]
    [com.sixsq.slipstream.auth.sign :as s]
    [clojure.string :as str]
    [clojure.java.io :as io]
    [environ.core :as environ]))

(deftest revoked-cookie-ok
  (let [revoked (t/revoked-cookie)]
    (is (map? revoked))
    (is (= "INVALID" (get-in revoked [:value]))))
  (let [k "cookie.name"
        revoked (t/revoked-cookie k)]
    (is (map? revoked))
    (is (= "INVALID" (get-in revoked [k :value])))))

(deftest claims-cookie-ok
  (with-redefs [environ/env env-fixture/env-map]
    (let [claims {:alpha "a", :beta "b", :gamma 3}
          cookie (t/claims-cookie claims)
          k "cookie.name"
          named-cookie (t/claims-cookie claims k)]
      (is (map? cookie))
      (is (not= "INVALID" (get-in cookie [:value])))
      (is (not (str/blank? (get-in cookie [:value :token])))) ;; FIXME: Remove :token.
      (is (map? named-cookie))
      (is (not= "INVALID" (get-in named-cookie [k :value])))
      (is (not (str/blank? (get-in named-cookie [k :value :token]))))))) ;; FIXME: Remove :token.
