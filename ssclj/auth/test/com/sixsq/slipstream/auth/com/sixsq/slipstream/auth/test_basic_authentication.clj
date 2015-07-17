(ns com.sixsq.slipstream.auth.com.sixsq.slipstream.auth.test-basic-authentication
  (:refer-clojure :exclude [update])
  (:require
    [korma.core                                       :as kc]
    [clojure.test                                     :refer :all]
    [com.sixsq.slipstream.auth.simple-authentication  :as sa]
    [com.sixsq.slipstream.auth.core                   :as c]))

(defn fixture-delete-all
  [f]
  (sa/init)
  (kc/delete sa/users)
  (f))

(use-fixtures :each fixture-delete-all)

(def sa (sa/get-instance))

(defn- rejected?
  [[ok? result]]
  (= [false {:message "Invalid username or password"}]
     [ok? result]))

(deftest test-add-user
  (c/add-user! sa {:user-name  "joe"
                  :password   "secret"})

  (is (=  [true {:user-name "joe"}]
          (c/auth-user sa { :user-name "joe"
                            :password  "secret"})))

  (is (rejected?
        (c/auth-user sa { :user-name "joe"
                          :password  "wrong"})))

  (is (rejected?
        (c/auth-user sa { :user-name "XXXXXX"
                          :password  "secret"}))))

