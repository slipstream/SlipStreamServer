(ns com.sixsq.slipstream.auth.cookies-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [clj-time.coerce :as c]
    [com.sixsq.slipstream.auth.env-fixture :as env-fixture]
    [com.sixsq.slipstream.auth.cookies :as t]
    [com.sixsq.slipstream.auth.utils.sign :as s]
    [clojure.string :as str]
    [clojure.java.io :as io]
    [environ.core :as environ]
    [ring.util.codec :as codec]))

(defn serialize-cookie-value
  "replaces the map cookie value with a serialized string"
  [{:keys [value] :as cookie}]
  (assoc cookie :value (codec/form-encode value)))

(defn damaged-cookie-value
  "replaces the map cookie value with a serialized string, but modifies it to make it invalid"
  [{:keys [value] :as cookie}]
  (assoc cookie :value (str (codec/form-encode value) "-INVALID")))

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
      (is (-> cookie :value :token))
      (is (map? named-cookie))
      (is (not= "INVALID" (get-in named-cookie [k :value])))
      (is (get-in named-cookie [k :value :token])))))

(deftest check-extract-claims
  (with-redefs [environ/env env-fixture/env-map]
    (let [claims {:alpha "a", :beta "b", :gamma 3}]
      (is (nil? (t/extract-claims nil)))
      (is (nil? (t/extract-claims {:value nil})))
      (is (thrown? Exception (t/extract-claims {:value "token=INVALID"})))
      (is (thrown? Exception (t/extract-claims {:value "unknown-token"})))
      (is (= claims (-> claims
                        t/claims-cookie
                        serialize-cookie-value
                        t/extract-claims
                        (dissoc :exp)))))))

(deftest check-claims->authn-info
  (are [expected claims] (= expected (t/claims->authn-info claims))
                         nil nil
                         nil {}
                         ["user" []] {:com.sixsq.identifier "user"}
                         ["user" ["role1"]] {:com.sixsq.identifier "user", :com.sixsq.roles "role1"}
                         ["user" ["role1" "role2"]] {:com.sixsq.identifier "user", :com.sixsq.roles "role1 role2"}))

(deftest check-extract-cookie-claims
  (with-redefs [environ/env env-fixture/env-map]
    (let [claims {:com.sixsq.identifier "user"
                  :com.sixsq.roles      "role1 role2"
                  :com.sixsq.session    "session/81469d29-40dc-438e-b60f-e8748e3c7ee6"}]

      (is (nil? (t/extract-cookie-claims nil)))
      (is (nil? (-> claims
                    t/claims-cookie
                    damaged-cookie-value
                    t/extract-cookie-claims
                    (dissoc :exp))))
      (is (= claims (-> claims
                        t/claims-cookie
                        serialize-cookie-value
                        t/extract-cookie-claims
                        (dissoc :exp)))))))

(deftest check-extract-cookie-info
  (with-redefs [environ/env env-fixture/env-map]
    (let [claims {:com.sixsq.identifier "user"
                  :com.sixsq.roles      "role1 role2"}]

      (is (nil? (t/extract-cookie-info nil)))
      (is (nil? (-> claims
                    t/claims-cookie
                    damaged-cookie-value
                    t/extract-cookie-info)))
      (is (= ["user" ["role1" "role2"]] (-> claims
                                            t/claims-cookie
                                            serialize-cookie-value
                                            t/extract-cookie-info))))))
