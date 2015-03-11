(ns com.sixsq.slipstream.ssclj.resources.common.schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.schema :refer :all]
    [schema.core :as s]
    [expectations :refer :all]))

;;
;; NotEmpty
;;

(expect nil? (s/check NotEmpty "ok"))
(expect nil? (s/check NotEmpty ["ok"]))
(expect nil? (s/check NotEmpty [1 2]))
(expect nil? (s/check NotEmpty {"ok" "value"}))
(expect nil? (s/check NotEmpty {1 2}))
(expect nil? (s/check NotEmpty "\t"))
(expect nil? (s/check NotEmpty " "))
(expect (s/check NotEmpty ""))
(expect (s/check NotEmpty []))
(expect (s/check NotEmpty {}))

;;
;; PosInt
;;

(expect nil? (s/check PosInt 1))
(expect nil? (s/check PosInt 2))
(expect nil? (s/check PosInt 3))
(expect (s/check PosInt 0))
(expect (s/check PosInt -1))
(expect (s/check PosInt 1.0))
(expect (s/check PosInt "bad"))

;;
;; NonNegInt
;;

(expect nil? (s/check NonNegInt 0))
(expect nil? (s/check NonNegInt 1))
(expect nil? (s/check NonNegInt 2))
(expect nil? (s/check NonNegInt 3))
(expect (s/check NonNegInt -1))
(expect (s/check NonNegInt 1.0))
(expect (s/check NonNegInt "bad"))

;;
;; NonBlankString
;;

(expect nil? (s/check NonBlankString "ok"))
(expect nil? (s/check NonBlankString " ok"))
(expect nil? (s/check NonBlankString "ok "))
(expect nil? (s/check NonBlankString " ok "))
(expect (s/check NonBlankString ""))
(expect (s/check NonBlankString " "))
(expect (s/check NonBlankString "\t"))
(expect (s/check NonBlankString "\f"))
(expect (s/check NonBlankString " \t\f"))

;;
;; NonEmptyStrlist
;;

(expect nil? (s/check NonEmptyStrList ["ok"]))
(expect nil? (s/check NonEmptyStrList ["ok" "ok"]))
(expect (s/check NonEmptyStrList []))
(expect (s/check NonEmptyStrList [1]))
(expect (s/check NonEmptyStrList ["ok" 1]))

;;
;; Timestamp
;;

(expect nil? (s/check Timestamp "2012-01-01T01:23:45.678Z"))
(expect (s/check Timestamp "2012-01-01T01:23:45.678Q"))

;;
;; ResourceLink
;;

(expect nil? (s/check ResourceLink {:href "uri"}))
(expect (s/check ResourceLink {}))
(expect (s/check ResourceLink {:bad "value"}))
(expect (s/check ResourceLink {:href ""}))
(expect (s/check ResourceLink {:href "uri" :bad "value"}))

;;
;; ResourceLinks
;;

(expect nil? (s/check ResourceLinks [{:href "uri"}]))
(expect nil? (s/check ResourceLinks [{:href "uri"} {:href "uri"}]))
(expect (s/check ResourceLinks []))

;;
;; Operation
;;

(expect nil? (s/check Operation {:href "uri" :rel "add"}))
(expect (s/check Operation {:href "uri"}))
(expect (s/check Operation {:rel "add"}))
(expect (s/check Operation {}))

;;
;; Operations
;;

(expect nil? (s/check Operations [{:href "uri" :rel "add"}]))
(expect nil? (s/check Operations [{:href "uri" :rel "add"} {:href "uri" :rel "delete"}]))
(expect (s/check Operations []))

;;
;; Properties
;;

(expect nil? (s/check Properties {:a "ok"}))
(expect nil? (s/check Properties {:a "ok" :b "ok"}))
(expect nil? (s/check Properties {"a" "ok"}))
(expect nil? (s/check Properties {"a" "ok" "b" "ok"}))
(expect (s/check Properties {}))
(expect (s/check Properties {1 "ok"}))
(expect (s/check Properties {"ok" 1}))
(expect (s/check Properties [:bad "bad"]))

;;
;; Access control schemas
;;

(def valid-acl {:owner {:principal "me" :type "USER"}})

(let [id {:principal "::ADMIN"
          :type      "ROLE"}]
  (expect nil? (s/check AccessControlId id))
  (expect (s/check AccessControlId (assoc id :bad "MODIFY")))
  (expect (s/check AccessControlId (dissoc id :principal)))
  (expect (s/check AccessControlId (dissoc id :type)))
  (expect (s/check AccessControlId (assoc id :type "BAD"))))

(let [rule {:principal "::ADMIN"
            :type      "ROLE"
            :right     "VIEW"}]
  (expect nil? (s/check AccessControlRule rule))
  (expect nil? (s/check AccessControlRule (assoc rule :right "MODIFY")))
  (expect nil? (s/check AccessControlRule (assoc rule :right "ALL")))
  (expect (s/check AccessControlRule (assoc rule :right "BAD")))
  (expect (s/check AccessControlRule (dissoc rule :right))))

(let [rules [{:principal "::ADMIN"
              :type      "ROLE"
              :right     "VIEW"}

             {:principal "ALPHA"
              :type      "USER"
              :right     "ALL"}]]
  (expect nil? (s/check AccessControlRules rules))
  (expect nil? (s/check AccessControlRules (next rules)))
  (expect (s/check AccessControlRules (next (next rules))))
  (expect (s/check AccessControlRules (cons 1 rules))))

(let [acl {:owner {:principal "::ADMIN"
                   :type      "ROLE"}
           :rules [{:principal ":group1"
                    :type      "ROLE"
                    :right     "VIEW"}
                   {:principal "group2"
                    :type      "ROLE"
                    :right     "MODIFY"}]}]
  (expect nil? (s/check AccessControlList acl))
  (expect nil? (s/check AccessControlList (dissoc acl :rules)))
  (expect (s/check AccessControlList (assoc acl :rules [])))
  (expect (s/check AccessControlList (assoc acl :owner "")))
  (expect (s/check AccessControlList (assoc acl :bad "BAD"))))

(expect nil? (s/check AclAttr {:acl valid-acl}))
(expect (s/check AclAttr {}))

;;
;; Common CIMI attributes
;;

(let [date "2012-01-01T01:23:45.678Z"
      minimal {:id          "a"
               :resourceURI "http://example.org/data"
               :created     date
               :updated     date}
      maximal (assoc minimal
                :name "name"
                :description "description"
                :properties {"a" "b"}
                :operations [{:rel "add" :href "/add"}])]
  (expect nil? (s/check CommonAttrs minimal))
  (expect (s/check CommonAttrs (dissoc minimal :id)))
  (expect (s/check CommonAttrs (dissoc minimal :resourceURI)))
  (expect (s/check CommonAttrs (dissoc minimal :created)))
  (expect (s/check CommonAttrs (dissoc minimal :updated)))

  (expect nil? (s/check CommonAttrs maximal))
  (expect nil? (s/check CommonAttrs (dissoc maximal :name)))
  (expect nil? (s/check CommonAttrs (dissoc maximal :description)))
  (expect nil? (s/check CommonAttrs (dissoc maximal :properties)))
  (expect (s/check CommonAttrs (assoc maximal :bad "BAD"))))


(run-tests [*ns*])

