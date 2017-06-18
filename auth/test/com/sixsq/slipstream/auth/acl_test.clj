;; old namespace: com.sixsq.slipstream.ssclj.resources.common.authz-test
(ns com.sixsq.slipstream.auth.acl-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.auth.acl :as acl]))

(deftest check-current-authentication
  (are [expect arg] (= expect (:identity (acl/current-authentication arg)))
                    nil {}
                    nil {:identity {:authentications {}}}
                    nil {:identity {:authentications {"user" {:identity "user"}}}}
                    nil {:identity {:current         "other"
                                    :authentications {"user" {:identity "user"}}}}
                    "user" {:identity {:current         "user"
                                       :authentications {"user" {:identity "user"}}}}))

(deftest check-extract-right

  (is (= ::acl/all (acl/extract-right {:identity "anyone" :roles ["R1", "ADMIN"]}
                                  {:type "USER" :principal "USER1" :right "ALL"})))

  (is (= ::acl/view (acl/extract-right nil {:type "ROLE" :principal "ANON" :right "VIEW"})))
  (is (= ::acl/view (acl/extract-right {} {:type "ROLE" :principal "ANON" :right "VIEW"})))

  (is (nil? (acl/extract-right {:identity "unknown" :roles ["ANON"]}
                           {:principal "USER" :type "ROLE" :right "MODIFY"})))

  (let [id-map {:identity "USER1" :roles ["R1" "R3"]}]

    (are [expect arg] (= expect (acl/extract-right id-map arg))
                      ::acl/all {:type "USER" :principal "USER1" :right "ALL"}
                      nil {:type "USER" :principal "USER1"}
                      nil {:type "ROLE" :principal "USER1" :right "ALL"}
                      ::acl/view {:type "ROLE" :principal "R1" :right "VIEW"}
                      nil {:type "USER" :principal "R1" :right "VIEW"}
                      nil {:type "ROLE" :principal "R2" :right "MODIFY"}
                      ::acl/modify {:type "ROLE" :principal "R3" :right "MODIFY"}
                      nil {:type "ROLE" :principal "R3"}
                      nil {:type "ROLE" :principal "ANON"}
                      ::acl/view {:type "ROLE" :principal "ANON" :right "VIEW"}
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

    (are [expect arg] (= expect (acl/extract-rights arg acl))
                      #{} nil
                      #{} {:identity nil}
                      #{::acl/all} {:identity "USER1"}
                      #{::acl/all ::acl/view} {:identity "USER1" :roles ["ROLE1"]}
                      #{} {:identity "USER_UNKNOWN" :roles ["ROLE_UNKNOWN"]}
                      #{::acl/view} {:identity "USER_UNKNOWN" :roles ["ROLE1"]}
                      #{::acl/view ::acl/modify} {:identity "USER2" :roles ["ROLE1"]})))

(deftest check-hierarchy
  (are [parent child] (isa? parent child)
                      ::acl/all ::acl/view
                      ::acl/modify ::acl/view
                      ::acl/modify ::acl/view)
  (are [parent child] (not (isa? parent child))
                      ::acl/view ::acl/all
                      ::acl/view ::acl/modify
                      ::acl/modify ::acl/all))

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

      (is (= resource (acl/can-do? resource request ::acl/all)))
      (is (= resource (acl/can-do? resource request ::acl/modify)))
      (is (= resource (acl/can-do? resource request ::acl/view)))

      (let [request {:identity {:current         "USER_UNKNOWN"
                                :authentications {"USER_UNKNOWN" {:identity "USER_UNKNOWN"
                                                                  :roles    ["ROLE1"]}}}}]

        (is (thrown? Exception (acl/can-do? resource request ::acl/all)))
        (is (thrown? Exception (acl/can-do? resource request ::acl/modify)))
        (is (= resource (acl/can-do? resource request ::acl/view))))

      (let [request {:identity {:current         "USER2"
                                :authentications {"USER2" {:identity "USER2"}}}}]

        (is (thrown? Exception (acl/can-do? resource request ::acl/all)))
        (is (= resource (acl/can-do? resource request ::acl/modify)))
        (is (= resource (acl/can-do? resource request ::acl/view))))

      (let [request {:identity {:current         "USER2"
                                :authentications {"USER2" {:identity "USER2"
                                                           :roles    ["ROLE1"]}}}}]

        (is (thrown? Exception (acl/can-do? resource request ::acl/all)))
        (is (= resource (acl/can-do? resource request ::acl/modify)))
        (is (= resource (acl/can-do? resource request ::acl/view)))))))
