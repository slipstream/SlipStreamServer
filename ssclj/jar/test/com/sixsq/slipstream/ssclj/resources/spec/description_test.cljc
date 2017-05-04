(ns com.sixsq.slipstream.ssclj.resources.spec.description-test
  (:require
    [clojure.test :refer [deftest are is]]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.description :as t]))

(deftest check-parameter-type
  (are [expect-fn arg] (expect-fn (s/valid? :cimi.desc/type arg))
                       true? "string"
                       true? "boolean"
                       true? "int"
                       true? "float"
                       true? "timestamp"
                       true? "enum"
                       true? "map"
                       true? "list"
                       false? "unknown"))

(deftest check-parameter-desd-and-resource-desc
  (let [valid-acl {:owner {:principal "me" :type "USER"}}
        valid-desc {:displayName "ID"
                    :category    "common"
                    :description "unique resource identifier"
                    :type        "enum"
                    :mandatory   true
                    :readOnly    true
                    :order       0
                    :enum        ["a" "b" "c"]}
        resource-desc {:identifier valid-desc
                       :other      valid-desc
                       :acl        valid-acl}]

    (are [expect-fn arg] (expect-fn (s/valid? :cimi.desc/parameter-description arg))
                         true? valid-desc
                         true? (dissoc valid-desc :category)
                         true? (dissoc valid-desc :description)
                         true? (dissoc valid-desc :mandatory)
                         true? (dissoc valid-desc :readOnly)
                         true? (dissoc valid-desc :order)
                         true? (dissoc valid-desc :enum)
                         false? (assoc valid-desc :displayName 1)
                         false? (assoc valid-desc :category 1)
                         false? (assoc valid-desc :description 1)
                         false? (assoc valid-desc :type "unknown")
                         false? (assoc valid-desc :mandatory 1)
                         false? (assoc valid-desc :readOnly 1)
                         false? (assoc valid-desc :readOnly "1")
                         false? (assoc valid-desc :enum "1")
                         false? (assoc valid-desc :enum ["a" 1]))

    (are [expect-fn arg] (expect-fn (s/valid? :cimi.desc/resource-description arg))
                         true? resource-desc
                         false? (assoc resource-desc :another 1)
                         true? (assoc t/CommonParameterDescription :acl valid-acl))))
