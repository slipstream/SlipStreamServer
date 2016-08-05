(ns slipstream.async.collector-test
  (:require
    [clojure.test :refer :all]
    [slipstream.async.collector :refer :all])
  (:import
    [com.sixsq.slipstream.connector.local LocalConnector]
    [com.sixsq.slipstream.connector.local LocalConnector]
    [com.sixsq.slipstream.persistence User]
    [com.sixsq.slipstream.persistence UserParameter]))

(deftest test-add-increasing-space
  (is (= [["joe" "exo" 0] ["joe" "aws" 10] ["mike" "exo" 20]]
         (add-increasing-space [["joe" "exo"] ["joe" "aws"] ["mike" "exo"]] 10)))
  (is (= [["joe" "exo" 0] ["joe" "aws" 1] ["mike" "exo" 2]]
         (add-increasing-space [["joe" "exo"] ["joe" "aws"] ["mike" "exo"]] 1)))
  )

(def connector (LocalConnector. "c1"))
(def user-no-connector (User. "foo"))
(def user-with-connector
  (doto (User. "foo")
    (.setParameter (UserParameter. "c1.username" "user" "Cloud username."))
    (.setParameter (UserParameter. "c1.password" "pass" "Cloud password."))))

(deftest test-compose-uc-pairs
  (is (= 0 (count (compose-uc-pairs [] []))))
  (is (= 0 (count (compose-uc-pairs [user-with-connector] []))))
  (is (= 0 (count (compose-uc-pairs [] [connector]))))
  (is (= 0 (count (compose-uc-pairs [user-no-connector] [connector]))))
  (is (= 1 (count (compose-uc-pairs [user-with-connector] [connector]))))
  (is (= 1 (count (compose-uc-pairs [user-no-connector user-with-connector] [connector]))))
  (is (= 2 (count (compose-uc-pairs [user-with-connector user-with-connector] [connector]))))
  (is (= 4 (count (compose-uc-pairs [user-with-connector user-with-connector] [connector connector]))))
  (is (= 2 (count (compose-uc-pairs [user-with-connector user-with-connector] [connector (LocalConnector. "c2")])))))

