(ns com.sixsq.slipstream.ssclj.resources.root-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.root :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id          resource-name
            :resourceURI c/service-context
            :created     timestamp
            :updated     timestamp
            :acl         resource-acl
            :baseURI     "http://cloud.example.org/"}]

  (expect nil? (s/check Root root))
  (expect nil? (s/check Root (assoc root :resources {:href "Resource/uuid"})))
  (expect (s/check Root (dissoc root :created)))
  (expect (s/check Root (dissoc root :updated)))
  (expect (s/check Root (dissoc root :baseURI)))
  (expect (s/check Root (dissoc root :acl))))


(run-tests [*ns*])

