(ns com.sixsq.slipstream.ssclj.resources.common.authz-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.authz :as authz :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

(expect nil? (current-authentication {}))
(expect nil? (current-authentication {:identity {:authentications {}}}))
(expect nil? (current-authentication {:identity {:authentications {"user" {:identity "user"}}}}))
(expect nil? (current-authentication {:identity {:current         "other"
                                                 :authentications {"user" {:identity "user"}}}}))
(expect "user" (:identity (current-authentication {:identity {:current         "user"
                                                              :authentications {"user" {:identity "user"}}}})))


(let [id-map {:identity "USER1"
              :roles    ["R1" "R3"]}]

  (expect ::authz/all (extract-right id-map {:principal "USER1" :type "USER" :right "ALL"}))
  (expect nil? (extract-right id-map {:principal "USER1" :type "USER"}))
  (expect nil? (extract-right id-map {:principal "USER1" :type "ROLE" :right "ALL"}))
  (expect ::authz/view (extract-right id-map {:principal "R1" :type "ROLE" :right "VIEW"}))
  (expect nil? (extract-right id-map {:principal "R1" :type "USER" :right "VIEW"}))
  (expect nil? (extract-right id-map {:principal "R2" :type "ROLE" :right "MODIFY"}))
  (expect ::authz/modify (extract-right id-map {:principal "R3" :type "ROLE" :right "MODIFY"}))
  (expect nil? (extract-right id-map {:principal "R3" :type "ROLE"}))

  (expect nil? (extract-right id-map {:principal "ANON" :type "ROLE"}))
  (expect ::authz/view (extract-right id-map {:principal "ANON" :type "ROLE" :right "VIEW"})))


(let [acl {:owner {:principal "USER1"
                   :type      "USER"}
           :rules [{:principal "ROLE1"
                    :type      "ROLE"
                    :right     "VIEW"}
                   {:principal "USER2"
                    :type      "USER"
                    :right     "MODIFY"}]}]

  (expect #{} (extract-rights {:identity nil} acl))
  (expect #{::authz/all} (extract-rights {:identity "USER1"} acl))
  (expect #{::authz/all ::authz/view} (extract-rights {:identity "USER1" :roles ["ROLE1"]} acl))
  (expect #{} (extract-rights {:identity "USER_UNKNOWN" :roles ["ROLE_UNKNOWN"]} acl))
  (expect #{::authz/view} (extract-rights {:identity "USER_UNKNOWN" :roles ["ROLE1"]} acl))
  (expect #{::authz/view ::authz/modify} (extract-rights {:identity "USER2" :roles ["ROLE1"]} acl)))

(expect (isa? ::authz/all ::authz/view))
(expect (isa? ::authz/modify ::authz/view))
(expect (isa? ::authz/view ::authz/view))

(expect false (isa? ::authz/view ::authz/all))
(expect false (isa? ::authz/view ::authz/modify))
(expect false (isa? ::authz/modify ::authz/all))

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

  (let [request {:identity {:current "USER1"
                            :authentications {"USER1" {:identity "USER1"}}}}]

    (expect resource (can-do? resource request ::authz/all))
    (expect resource (can-do? resource request ::authz/modify))
    (expect resource (can-do? resource request ::authz/view)))

  (let [request {:identity {:current "USER_UNKNOWN"
                            :authentications {"USER_UNKNOWN" {:identity "USER_UNKNOWN"
                                                              :roles ["ROLE1"]}}}}]

    (expect Exception (can-do? resource request ::authz/all))
    (expect Exception (can-do? resource request ::authz/modify))
    (expect resource (can-do? resource request ::authz/view)))

  (let [request {:identity {:current "USER2"
                            :authentications {"USER2" {:identity "USER2"}}}}]

    (expect Exception (can-do? resource request ::authz/all))
    (expect resource (can-do? resource request ::authz/modify))
    (expect resource (can-do? resource request ::authz/view)))

  (let [request {:identity {:current "USER2"
                            :authentications {"USER2" {:identity "USER2"
                                                       :roles ["ROLE1"]}}}}]

    (expect Exception (can-do? resource request ::authz/all))
    (expect resource (can-do? resource request ::authz/modify))
    (expect resource (can-do? resource request ::authz/view))))


(run-tests [*ns*])

