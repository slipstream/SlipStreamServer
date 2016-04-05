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

;; SessionCredentials
(expect nil? (s/check SessionCredentials {:username "username"}))
(expect nil? (s/check SessionCredentials {:username "username"
                                          :password "password"}))
(expect (s/check SessionCredentials {}))
(expect (s/check SessionCredentials {"bad" "entry"}))
(expect (s/check SessionCredentials {:bad 1}))


;; SessionTemplate
(let [timestamp "1964-08-25T10:00:00.0Z"
      root {:id           resource-name
            :resourceURI  p/service-context
            :created      timestamp
            :updated      timestamp
            :acl          valid-acl
            :authn-method "internal"
            :logo         {:href "public/logo.png"}
            :credentials  {:user-name "user"
                           :password  "password"}}]

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


;; SessionTemplateAttrs and SessionTemplateRef
(let [tpl-attrs {:authn-method resource-name
                 :logo         {:href "public/logo.png"}
                 :credentials  {:user-name "user"
                                :password  "password"}}
      tpl-ref (merge tpl-attrs {:href "template/uuid"})]

  (expect nil? (s/check SessionTemplateAttrs {}))
  (expect nil? (s/check SessionTemplateAttrs tpl-attrs))
  (expect nil? (s/check SessionTemplateAttrs (dissoc tpl-attrs :authn-method)))
  (expect nil? (s/check SessionTemplateAttrs (dissoc tpl-attrs :logo)))
  (expect nil? (s/check SessionTemplateAttrs (dissoc tpl-attrs :credentials)))

  (expect (s/check SessionTemplateRef {}))
  (expect nil? (s/check SessionTemplateRef tpl-ref))
  (expect nil? (s/check SessionTemplateRef (dissoc tpl-ref :href)))
  (expect nil? (s/check SessionTemplateRef (dissoc tpl-ref :authn-method)))
  (expect nil? (s/check SessionTemplateRef (dissoc tpl-ref :logo)))
  (expect nil? (s/check SessionTemplateRef (dissoc tpl-ref :credentials))))


(run-tests [*ns*])
