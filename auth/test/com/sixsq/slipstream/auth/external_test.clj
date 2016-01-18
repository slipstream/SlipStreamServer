(ns com.sixsq.slipstream.auth.external-test
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.external :refer :all]
    [com.sixsq.slipstream.auth.test-helper :as th]))

(defn fixture-delete-all
  [f]
  (th/create-test-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(deftest match-new-user-github
  (is (= [] (kc/select db/users)))
  (match-external-user! :github "st" "st@sixsq.com")
  (let [created-user (first (kc/select db/users))]
    (is (= {:DELETED     false
            :EMAIL       "st@sixsq.com"
            :CYCLONELOGIN nil
            :GITHUBLOGIN "st"
            :ISSUPERUSER false
            :JPAVERSION  0
            :NAME        "st"
            :RESOURCEURI "user/st"
            :STATE       "ACTIVE"}
           (dissoc created-user :CREATION :PASSWORD)))))

(deftest match-new-user-cyclone
  (is (= [] (kc/select db/users)))
  (match-external-user! :cyclone "st" "st@sixsq.com")
  (let [created-user (first (kc/select db/users))]
    (is (= {:DELETED     false
            :EMAIL       "st@sixsq.com"
            :CYCLONELOGIN "st"
            :GITHUBLOGIN nil
            :ISSUPERUSER false
            :JPAVERSION  0
            :NAME        "st"
            :RESOURCEURI "user/st"
            :STATE       "ACTIVE"}
           (dissoc created-user :CREATION :PASSWORD)))))

(deftest match-existing-user
  (db/create-user! :github nil "st@sixsq.com")
  (let [users-before-match (kc/select db/users)]
    (is (= 1 (count users-before-match)))
    (is (nil? (:GITHUBLOGIN (first users-before-match)))))
  (match-external-user! :github "st" "st@sixsq.com")
  (let [users-after-match (kc/select db/users)]
    (is (= 1 (count users-after-match)))
    (is (= "st" (:GITHUBLOGIN (first users-after-match))))))

(deftest match-already-mapped
  (db/create-user! :github "st" "st@sixsq.com")
  (let [users-before-match (kc/select db/users)]
    (match-external-user! :github "st" "st@sixsq.com")
    (is (= users-before-match (kc/select db/users)))))