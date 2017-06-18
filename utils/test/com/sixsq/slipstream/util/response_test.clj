(ns com.sixsq.slipstream.util.response-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.util.response :as r]))

(deftest check-response-created
  (let [id "RESOURCE_ID"
        r (r/response-created id)]
    (is (= 201 (:status r)))
    (is (= id (get-in r [:headers "Location"])))
    (is (nil? (:cookies r))))

  (let [id "RESOURCE_ID"
        cookie-name "MY_COOKIE"
        cookie-value "MY_COOKIE_VALUE"
        r (r/response-created id [cookie-name cookie-value])]
    (is (= 201 (:status r)))
    (is (= id (get-in r [:headers "Location"])))
    (is (= cookie-value (get-in r [:cookies cookie-name])))))
