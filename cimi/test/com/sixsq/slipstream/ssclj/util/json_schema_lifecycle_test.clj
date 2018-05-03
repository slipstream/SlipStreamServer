(ns com.sixsq.slipstream.ssclj.util.json-schema-lifecycle-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.ssclj.util.json-schema :as t]
            [com.sixsq.slipstream.ssclj.resources.spec.common :as common]
            ))



(deftest common-schema

  (is (= (t/transform common/common-attrs) {}   ))

  (is (= (t/transform ::common/acl) {:type "object",
                                     :properties {"owner" {:type "object",
                                                           :properties {"principal" {:type "keyword"}, "type" {:enum ["USER" "ROLE"]}},
                                                           :required ["principal" "type"]},
                                                  "rules" {:type "array",
                                                           :items {:type "object",
                                                                   :properties {"principal" {:type "keyword"},
                                                                                "type" {:enum ["USER" "ROLE"]},
                                                                                "right" {:enum ["DELETE"
                                                                                                "MANAGE"
                                                                                                "VIEW_DATA"
                                                                                                "EDIT_META"
                                                                                                "MODIFY"
                                                                                                "VIEW"
                                                                                                "VIEW_META"
                                                                                                "VIEW_ACL"
                                                                                                "ALL"
                                                                                                "EDIT_DATA"
                                                                                                "EDIT_ACL"]}},
                                                                   :required ["principal" "right" "type"]}}},
                                     :required ["owner"]}   ))

  )