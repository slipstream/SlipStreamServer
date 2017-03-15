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
      (is (not= "INVALID" (:value cookie)))
      (is (re-matches #"^token=.*" (:value cookie)))           ;; FIXME: Remove token=.
      (is (map? named-cookie))
      (is (not= "INVALID" (get-in named-cookie [k :value])))
      (is (re-matches #"^token=.*" (get-in named-cookie [k :value])))))) ;; FIXME: Remove token=.

(deftest check-extract-claims
  (with-redefs [environ/env env-fixture/env-map]
    (let [claims {:alpha "a", :beta "b", :gamma 3}
          cookie (t/claims-cookie claims)]
      (is (nil? (t/extract-claims nil)))
      (is (nil? (t/extract-claims {:value nil})))
      (is (thrown? Exception (t/extract-claims {:value "token=INVALID"})))
      (is (thrown? Exception (t/extract-claims {:value "unknown-token"})))
      (is (= claims (-> claims
                        t/claims-cookie
                        t/extract-claims
                        (dissoc :exp)))))))

(deftest check-claims->authn-info
  (are [expected claims] (= expected (t/claims->authn-info claims))
                         nil nil
                         nil {}
                         ["user" []] {:com.sixsq.identifier "user"}
                         ["user" ["role1"]] {:com.sixsq.identifier "user", :com.sixsq.roles "role1"}
                         ["user" ["role1" "role2"]] {:com.sixsq.identifier "user", :com.sixsq.roles "role1 role2"}))

(deftest check-extract-cookie-info
  (with-redefs [environ/env env-fixture/env-map]
    (let [claims {:com.sixsq.identifier "user"
                  :com.sixsq.roles      "role1 role2"}]

      (is (= ["user" ["role1" "role2"]] (-> claims
                                            t/claims-cookie
                                            t/extract-cookie-info))))))
