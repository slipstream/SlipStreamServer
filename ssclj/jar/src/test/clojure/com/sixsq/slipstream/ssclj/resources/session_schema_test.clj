(ns com.sixsq.slipstream.ssclj.resources.session-schema-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.session :refer :all]
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.params :as p]))

(def valid-acl {:owner {:principal "::ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "::ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})

;; Session
(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id           resource-name
            :resourceURI  p/service-context
            :created      timestamp
            :updated      timestamp
            :acl          valid-acl
            :username     "joe_user"
            :authn-method "internal"
            :last-active  timestamp}]

  (expect nil? (s/check Session root))

  (expect (s/check Session (dissoc root :resourceURI)))
  (expect (s/check Session (dissoc root :created)))
  (expect (s/check Session (dissoc root :updated)))
  (expect (s/check Session (dissoc root :acl)))

  (expect (s/check Session (dissoc root :username)))
  (expect (s/check Session (dissoc root :authn-method)))
  (expect (s/check Session (dissoc root :last-active)))

  (expect (s/check Session (assoc root :last-active "invalid timestamp"))))


;; SessionCreate
(let [timestamp "1964-08-25T10:00:00.0Z"
      template {:href         "sessionTemplate/uuid"
                :authn-method "internal"
                :logo         {:href "media/uuid"}
                :credentials  {:username "joe-username"
                               :password "joe-password"}}
      root {:name            "session-template-test"
            :description     "test of session template"
            :resourceURI     p/service-context
            :created         timestamp
            :updated         timestamp
            :sessionTemplate template}]

  (expect nil? (s/check SessionCreate root))
  (expect nil? (s/check SessionCreate (dissoc root :name)))
  (expect nil? (s/check SessionCreate (dissoc root :description)))
  (expect nil? (s/check SessionCreate (dissoc root :created)))
  (expect nil? (s/check SessionCreate (dissoc root :updated)))

  (expect nil? (s/check SessionCreate (assoc root :sessionTemplate (dissoc template :href))))
  (expect nil? (s/check SessionCreate (assoc root :sessionTemplate (dissoc template :authn-method))))
  (expect nil? (s/check SessionCreate (assoc root :sessionTemplate (dissoc template :logo))))
  (expect nil? (s/check SessionCreate (assoc root :sessionTemplate (dissoc template :credentials))))

  (expect (s/check SessionCreate (dissoc root :resourceURI)))
  (expect (s/check SessionCreate (dissoc root :sessionTemplate)))
  (expect (s/check SessionCreate (assoc root :sessionTemplate {}))))


(run-tests [*ns*])

