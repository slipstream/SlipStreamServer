(ns com.sixsq.slipstream.auth.db-test
  (:refer-clojure :exclude [update])
  (:require
    [clojure.test :refer :all]
    [korma.core :as kc]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.auth.test-helper :as th]))

(defn fixture-delete-all
  [f]
  (th/create-fake-empty-user-table)
  (f))

(use-fixtures :each fixture-delete-all)

(deftest test-user-creation
  (is (= "github-st" (db/create-user "github" "st" "st@s.com")))
  (let [users-created (kc/select db/users)]
    (is (= 1 (count users-created)))
    (is (= {:AUTHNID     "st"
            :AUTHNMETHOD "github"
            :DELETED     false
            :EMAIL       "st@s.com"
            :ISSUPERUSER false
            :JPAVERSION  0
            :NAME        "github-st"
            :PASSWORD    nil
            :RESOURCEURI "user/github-st"
            :STATE       "ACTIVE"}
           (-> users-created first (dissoc :CREATION))))))
