(ns com.sixsq.slipstream.auth.machine-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.machine :as t]))

(deftest check-json-parsing
  (is (= {:alpha 3, :beta true, :gamma "OK"} (t/parse-json "{\"alpha\": 3, \"beta\": true, \"gamma\": \"OK\"}")))
  (is (nil? (t/parse-json nil)))
  (is (nil? (t/parse-json "")))
  (is (nil? (t/parse-json "INVALID"))))

(deftest check-create-token
  (is (t/create-token {:alpha 1, :beta true, :gamma "OK"}))
  (is (nil? (t/create-token nil))))

(deftest check-valid-token?
  (let [token (t/create-token {:alpha 1, :beta true, :gamma "OK"})]
    (is (t/valid-token? token))
    (is (not (t/valid-token? nil)))
    (is (not (t/valid-token? "")))
    (is (not (t/valid-token? "INVALID")))))

(deftest check-machine-token
  (let [auth-token (t/create-token {:alpha 1, :beta true, :gamma "OK"})
        machine-claims "{\"com.sixsq.identifier\": \"machine\",
                         \"com.sixsq.roles\": \"USER ANON\",
                         \"com.sixsq.machine\": true}"]
    (is (= 401 (:status (t/machine-token {:params {:claims machine-claims :token "INVALID"}}))))
    (is (= 400 (:status (t/machine-token {:params {:claims "INVALID_CLAIMS" :token auth-token}}))))
    (let [ok-response (t/machine-token {:params {:claims machine-claims :token auth-token}})]
      (is (= 200 (:status ok-response)))
      (is (:body ok-response)))))


