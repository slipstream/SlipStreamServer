(ns com.sixsq.slipstream.ssclj.resources.spec.util.spec-tools-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [qbits.spandex :as spandex]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.util.spec-tools :as t]
    )
  )

(deftest schema-with-array
  (are [input expect] (= (t/remove-array-schema input) expect)
                      :key :key
                      "value" "value"
                      {:k "v"} {:k "v"}
                      {:type "array"} {:type "array"}
                      {:type "array" :items "wrong"} {:type "array" :items "wrong"}
                      {:type "array" :items {:type :wrong}} {:type "array" :items {:type :wrong}}
                      {:type "array" :items {:type "ok"}} {:type "ok"}
                      {:type "array" :items {:type "ok" :other "other"}} {:type "ok" :other "other"}
                      ))

(deftest required-keyword
  (are [input expect] (= (t/remove-required-key input) expect)
                      :key :key
                      "value" "value"
                      {:k "v"} {:k "v"}
                      {:k "v" :required "required"} {:k "v"}
                      {:required "required"} {}
                      ))

(deftest transform
  (are [input expect] (= (t/transform input) expect)
                      :key :key
                      "value" "value"
                      {:k "v"} {:k "v"}
                      {} {}
                      nil nil
                      {:k "v" :required "required"} {:k "v"}
                      {:required "required"} {}
                      {:type "array" :items {:type "ok" :other "other"}} {:type "ok" :other "other"}
                      ))

(deftest mappings
  (let [empty {:mappings {:_doc {:properties {}}}}]
  (are [input expect] (= (t/spec->es-mapping input) expect)
                      (s/def :test/bool boolean?) empty
                      ::cimi-core/email empty
                      ::cimi-core/identifier empty
                      ::cimi-core/resource-href empty
                      ::c/acl {:mappings {:_doc {:properties {:owner {:type "object",
                                                              :properties {"principal" {:type "string"}, "type" {:enum ["USER" "ROLE"]}}},
                                                      :rules {:type "object",
                                                              :properties {"principal" {:type "string"},
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
                                                                                           "EDIT_ACL"]}}}}}}}
                      ::c/created empty
                      ::c/operations empty
                      ::c/resource-links empty
                      (s/def :cimi.test/common-attrs (su/only-keys-maps c/common-attrs)) {:mappings {:_doc {:properties {:description {:type "string"},
                                                                                                                         :properties {:type "object",
                                                                                                                                      :additionalProperties {:type "string"},
                                                                                                                                      :title "com.sixsq.slipstream.ssclj.resources.spec.common/properties"},
                                                                                                                         :updated {:type "string"},
                                                                                                                         :name {:type "string"},
                                                                                                                         :created {:type "string"},
                                                                                                                         :id {:type "string"},
                                                                                                                         :acl {:type "object",
                                                                                                                               :properties {"owner" {:type "object",
                                                                                                                                                     :properties {"principal" {:type "string"},
                                                                                                                                                                  "type" {:enum ["USER" "ROLE"]}}},
                                                                                                                                            "rules" {:type "object",
                                                                                                                                                     :properties {"principal" {:type "string"},
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
                                                                                                                                                                                  "EDIT_ACL"]}}}}},
                                                                                                                         :operations {:type "object",
                                                                                                                                      :properties {"href" {:type "string"}, "rel" {:type "string"}}},
                                                                                                                         :resourceURI {:type "string"}}}}}
                      )))

