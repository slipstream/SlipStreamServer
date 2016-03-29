(ns com.sixsq.slipstream.ssclj.resources.session-template-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.session-template :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id          resource-name
            :resourceURI p/service-context
            :created     timestamp
            :updated     timestamp
            :acl         valid-acl
            :authn-method "internal"
            :logo {:href "public/logo.png"}
            :credentials {:user-name "user"
                          :password "password"}}]

  (expect nil? (s/check SessionTemplate root))
  (expect (s/check SessionTemplate (dissoc root :created)))
  (expect (s/check SessionTemplate (dissoc root :updated)))
  (expect (s/check SessionTemplate (dissoc root :authn-method)))
  (expect nil? (s/check SessionTemplate (dissoc root :logo)))
  (expect (s/check SessionTemplate (dissoc root :acl)))
  (expect (s/check SessionTemplate (assoc root :unknown "value")))
  (expect nil? (s/check SessionTemplate (dissoc root :credentials)))
  (expect nil? (s/check SessionTemplate (assoc root :credentials {:a "a" :b "b" :c "c"})))
  (expect (s/check SessionTemplate (assoc root :credentials {})))
  (expect (s/check SessionTemplate (assoc root :credentials {"invalid" "key"}))))

(run-tests [*ns*])
