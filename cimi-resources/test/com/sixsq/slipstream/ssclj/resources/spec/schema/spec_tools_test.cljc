(ns com.sixsq.slipstream.ssclj.resources.spec.schema.spec-tools-test
  (:require
    [clojure.test :refer :all]
    [clojure.spec.alpha :as s]
    [qbits.spandex :as spandex]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.util.spec :as su]
    [com.sixsq.slipstream.ssclj.resources.spec.schema.spec-tools :as t]))

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

(deftest remove-required-keyword
  (are [input expect] (= (t/remove-required-key input) expect)
                      :key :key
                      "value" "value"
                      {:k "v"} {:k "v"}
                      {:k "v" :required "required"} {:k "v"}
                      {:required "required"} {}
                      ))

(deftest remove-title-keyword
  (are [input expect] (= (t/remove-title-key input) expect)
                      :key :key
                      "value" "value"
                      {:k "v"} {:k "v"}
                      {:k "v" :title "title"} {:k "v"}
                      {:title "title"} {}
                      ))


(deftest deprecated-string-type
  (are [input expect] (= (t/deprecated-string-type input) expect)
                      :key :key
                      "value" "value"
                      {:k "v"} {:k "v"}
                      {:k "v" :type "other"} {:k "v" :type "other"}
                      {:type "other"} {:type "other"}
                      {:type "string"} {:type "keyword"}
                      {:type "string" :other "other"}{:type "keyword" :other "other"}
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
                      {:embedded {:required "req"}} {:embedded {}}
                      {:type "array" :items {:type "ok" :other "other"}} {:type "ok" :other "other"}
                      {:properties {:example {:type "string" :title "a title"}}} {:properties {:example {:type "keyword"}}}
                      ))

(deftest mappings
  (let [empty {:mappings {:_doc {:properties {}}}}]
  (are [input expect] (= (t/spec->es-mapping input) expect)
                      (s/def :test/bool boolean?) empty
                      ::cimi-core/email empty
                      ::cimi-core/identifier empty
                      ::cimi-core/resource-href empty
                      ::c/acl {:mappings {:_doc {:properties {:owner {:type "object",
                                                                      :properties {"principal" {:type "keyword"}, "type" {:enum ["USER" "ROLE"]}}},
                                                              :rules {:type "object",
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
                                                                                                   "EDIT_ACL"]}}}}}}}
                      ::c/created empty
                      ::c/operations empty
                      ::c/resource-links empty
                      (s/def :cimi.test/common-attrs (su/only-keys-maps c/common-attrs)) {:mappings {:_doc {:properties {:description {:type "keyword"},
                                                                                                                         :properties {:type "object",
                                                                                                                                      :additionalProperties {:type "keyword"}}
                                                                                                                         :updated {:type "keyword"},
                                                                                                                         :name {:type "keyword"},
                                                                                                                         :created {:type "keyword"},
                                                                                                                         :id {:type "keyword"},
                                                                                                                         :acl {:type "object",
                                                                                                                               :properties {"owner" {:type "object",
                                                                                                                                                     :properties {"principal" {:type "keyword"},
                                                                                                                                                                  "type" {:enum ["USER" "ROLE"]}}},
                                                                                                                                            "rules" {:type "object",
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
                                                                                                                                                                                  "EDIT_ACL"]}}}}},
                                                                                                                         :operations {:type "object",
                                                                                                                                      :properties {"href" {:type "keyword"}, "rel" {:type "keyword"}}},
                                                                                                                         :resourceURI {:type "keyword"}}}}}
                      )))

