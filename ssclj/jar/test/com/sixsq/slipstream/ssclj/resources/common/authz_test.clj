(ns com.sixsq.slipstream.ssclj.resources.common.authz-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as authz :refer :all]))

(deftest check-current-authentication
  (are [expect arg] (= expect (:identity (current-authentication arg)))
                    nil {}
                    nil {:identity {:authentications {}}}
                    nil {:identity {:authentications {"user" {:identity "user"}}}}
                    nil {:identity {:current         "other"
                                    :authentications {"user" {:identity "user"}}}}
                    "user" {:identity {:current         "user"
                                       :authentications {"user" {:identity "user"}}}}))

(deftest check-extract-right

  (is (= ::authz/all (extract-right {:identity "anyone" :roles ["R1", "ADMIN"]}
                                    {:type "USER" :principal "USER1" :right "ALL"})))

  (is (= ::authz/view (extract-right nil {:type "ROLE" :principal "ANON" :right "VIEW"})))
  (is (= ::authz/view (extract-right {} {:type "ROLE" :principal "ANON" :right "VIEW"})))

  (let [id-map {:identity "USER1" :roles ["R1" "R3"]}]

    (are [expect arg] (= expect (extract-right id-map arg))
                      ::authz/all {:type "USER" :principal "USER1" :right "ALL"}
                      nil {:type "USER" :principal "USER1"}
                      nil {:type "ROLE" :principal "USER1" :right "ALL"}
                      ::authz/view {:type "ROLE" :principal "R1" :right "VIEW"}
                      nil {:type "USER" :principal "R1" :right "VIEW"}
                      nil {:type "ROLE" :principal "R2" :right "MODIFY"}
                      ::authz/modify {:type "ROLE" :principal "R3" :right "MODIFY"}
                      nil {:type "ROLE" :principal "R3"}
                      nil {:type "ROLE" :principal "ANON"}
                      ::authz/view {:type "ROLE" :principal "ANON" :right "VIEW"}
                      nil nil
                      nil {}))

  (let [acl {:owner {:principal "USER1"
                     :type      "USER"}
             :rules [{:principal "ROLE1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "USER2"
                      :type      "USER"
                      :right     "MODIFY"}]}]

    (are [expect arg] (= expect (extract-rights arg acl))
                      #{} nil
                      #{} {:identity nil}
                      #{::authz/all} {:identity "USER1"}
                      #{::authz/all ::authz/view} {:identity "USER1" :roles ["ROLE1"]}
                      #{} {:identity "USER_UNKNOWN" :roles ["ROLE_UNKNOWN"]}
                      #{::authz/view} {:identity "USER_UNKNOWN" :roles ["ROLE1"]}
                      #{::authz/view ::authz/modify} {:identity "USER2" :roles ["ROLE1"]})))

(deftest check-hierarchy
  (are [parent child] (isa? parent child)
                      ::authz/all ::authz/view
                      ::authz/modify ::authz/view
                      ::authz/modify ::authz/view)
  (are [parent child] (not (isa? parent child))
                      ::authz/view ::authz/all
                      ::authz/view ::authz/modify
                      ::authz/modify ::authz/all))

(deftest check-can-do?
  (let [acl {:owner {:principal "USER1"
                     :type      "USER"}
             :rules [{:principal "ROLE1"
                      :type      "ROLE"
                      :right     "VIEW"}
                     {:principal "USER2"
                      :type      "USER"
                      :right     "MODIFY"}]}
        resource {:acl         acl
                  :resource-id "Resource/uuid"}]

    (let [request {:identity {:current         "USER1"
                              :authentications {"USER1" {:identity "USER1"}}}}]

      (is (= resource (can-do? resource request ::authz/all)))
      (is (= resource (can-do? resource request ::authz/modify)))
      (is (= resource (can-do? resource request ::authz/view)))

      (let [request {:identity {:current         "USER_UNKNOWN"
                                :authentications {"USER_UNKNOWN" {:identity "USER_UNKNOWN"
                                                                  :roles    ["ROLE1"]}}}}]

        (is (thrown? Exception (can-do? resource request ::authz/all)))
        (is (thrown? Exception (can-do? resource request ::authz/modify)))
        (is (= resource (can-do? resource request ::authz/view))))

      (let [request {:identity {:current         "USER2"
                                :authentications {"USER2" {:identity "USER2"}}}}]

        (is (thrown? Exception (can-do? resource request ::authz/all)))
        (is (= resource (can-do? resource request ::authz/modify)))
        (is (= resource (can-do? resource request ::authz/view))))

      (let [request {:identity {:current         "USER2"
                                :authentications {"USER2" {:identity "USER2"
                                                           :roles    ["ROLE1"]}}}}]

        (is (thrown? Exception (can-do? resource request ::authz/all)))
        (is (= resource (can-do? resource request ::authz/modify)))
        (is (= resource (can-do? resource request ::authz/view)))))))
