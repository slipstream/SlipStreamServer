(ns com.sixsq.slipstream.ssclj.resources.session-oidc.utils-test
  (:require
    [clojure.test :refer [deftest are is]]
    [com.sixsq.slipstream.ssclj.resources.session-oidc.utils :as t]))

(deftest check-prefix
  (is (= "a:b" (t/prefix "a" "b")))
  (is (nil? (t/prefix nil "b")))
  (is (nil? (t/prefix "a" nil)))
  (is (nil? (t/prefix nil nil))))

(deftest check-extract-roles
  (is (= [] (t/extract-roles nil)))
  (is (= [] (t/extract-roles {})))
  (is (= [] (t/extract-roles {:realm "a"})))
  (is (= [] (t/extract-roles {:roles []})))
  (is (= [] (t/extract-roles {:realm "a" :roles []})))
  (is (= ["a:a"] (t/extract-roles {:realm "a" :roles ["a"]})))
  (is (= ["a:a" "a:b"] (t/extract-roles {:realm "a" :roles ["a" "b"]}))))

(deftest check-group-hierarchy
  (is (= [] (t/group-hierarchy nil)))
  (is (= [] (t/group-hierarchy "")))
  (is (= [] (t/group-hierarchy "/")))
  (is (= [] (t/group-hierarchy "//")))
  (is (= ["/a"] (t/group-hierarchy "a")))
  (is (= ["/a" "/a/b"] (t/group-hierarchy "/a/b")))
  (is (= ["/a" "/a/b"] (t/group-hierarchy "a//b")))
  (is (= ["/a" "/a/b" "/a/b/c"] (t/group-hierarchy "/a/b/c"))))

(deftest check-extract-groups
  (is (= [] (t/extract-groups nil)))
  (is (= [] (t/extract-groups {})))
  (is (= [] (t/extract-groups {:realm "a"})))
  (is (= [] (t/extract-groups {:groups []})))
  (is (= [] (t/extract-groups {:realm "a" :groups []})))
  (is (= ["a:/a"] (t/extract-groups {:realm "a" :groups ["/a"]})))
  (is (= ["a:/a" "a:/a/b"] (t/extract-groups {:realm "a" :groups ["/a/b"]})))
  (is (= ["a:/a" "a:/a/b" "a:/other"] (t/extract-groups {:realm "a" :groups ["/a/b" "/other"]}))))
