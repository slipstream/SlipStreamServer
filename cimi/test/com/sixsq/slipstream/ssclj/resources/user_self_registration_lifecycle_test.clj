(ns com.sixsq.slipstream.ssclj.resources.user-self-registration-lifecycle-test
  (:require
    [clojure.test :refer :all]
    [clojure.data.json :as json]
    [peridot.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.user :as user]
    [com.sixsq.slipstream.ssclj.resources.user-template :as ct]
    [com.sixsq.slipstream.ssclj.resources.user-template-self-registration :as self]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]
    [com.sixsq.slipstream.ssclj.middleware.authn-info-header :refer [authn-info-header]]
    [com.sixsq.slipstream.ssclj.app.params :as p]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [expound.alpha :as expound]))

(use-fixtures :each ltu/with-test-server-fixture)

(def base-uri (str p/service-context (u/de-camelcase user/resource-name)))


(deftest lifecycle
  (let [uname "120720737412@eduid.chhttps://eduid.ch/idp/shibboleth!https://fed-id.nuv.la/samlbridge/module.php/saml/sp/metadata.php/sixsq-saml-bridge!iqqrh4oiyshzcw9o40cvo0+pgka="
        href (str ct/resource-url "/" self/registration-method)
        template-url (str p/service-context ct/resource-url "/" self/registration-method)

        session (-> (ltu/ring-app)
                    session
                    (content-type "application/json"))
        session-admin (header session authn-info-header "root ADMIN")
        session-user (header session authn-info-header (format "%s USER ANON" uname))
        session-anon (header session authn-info-header "unknown ANON")

        name-attr "name"
        description-attr "description"
        properties-attr {:a "one", :b "two"}
        plaintext-password "plaintext-password"

        template (-> session-admin
                     (request template-url)
                     (ltu/body->edn)
                     (ltu/is-status 200)
                     (get-in [:response :body]))

        no-href-create {:userTemplate (ltu/strip-unwanted-attrs (assoc template
                                                                  :username uname
                                                                  :password plaintext-password
                                                                  :passwordRepeat plaintext-password
                                                                  :emailAddress "user@example.org"))}
        href-create {:name         name-attr
                     :description  description-attr
                     :properties   properties-attr
                     :resourceURI  "http://sixsq.com/slipstream/1/UserTemplateCreate" ;; FIXME: Why is this needed?
                     :userTemplate {:href           href
                                    :username       uname
                                    :password       plaintext-password
                                    :passwordRepeat plaintext-password
                                    :emailAddress   "jane@example.org"}}
        invalid-create (assoc-in href-create [:userTemplate :href] "user-template/unknown-template")]


    ;; anonymous user collection query should succeed but be empty
    ;; access needed to allow self-registration
    (-> session-anon
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; admin user collection query should succeed but be empty (no users created yet)
    (-> session-admin
        (request base-uri)
        (ltu/body->edn)
        (ltu/is-status 200)
        (ltu/is-count zero?)
        (ltu/is-operation-present "add")
        (ltu/is-operation-absent "delete")
        (ltu/is-operation-absent "edit"))

    ;; create a new user as administrator; fail without reference
    (-> session-admin
        (request base-uri
                 :request-method :post
                 :body (json/write-str no-href-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; anonymous create without template reference fails
    (-> session-anon
        (request base-uri
                 :request-method :post
                 :body (json/write-str no-href-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; admin create with invalid template fails
    (-> session-admin
        (request base-uri
                 :request-method :post
                 :body (json/write-str invalid-create))
        (ltu/body->edn)
        (ltu/is-status 400))

    ;; create a user anonymously
    (clojure.pprint/pprint href-create)
    (let [resp (-> session-anon
                   (request base-uri
                            :request-method :post
                            :body (json/write-str href-create))
                   (ltu/body->edn)
                   (ltu/dump)
                   (ltu/is-status 201))
          id (get-in resp [:response :body :resource-id])
          uri (-> resp
                  (ltu/location))
          abs-uri (str p/service-context (u/de-camelcase uri))]

      ;; creating same user a second time should fail
      (-> session-anon
          (request base-uri
                   :request-method :post
                   :body (json/write-str href-create))
          (ltu/body->edn)
          (ltu/is-status 409))

      ;; admin should be able to see, edit, and delete user
      (-> session-admin
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-present "edit"))

      (-> session-user
          (request abs-uri)
          (ltu/body->edn)
          (ltu/is-status 200)
          (ltu/is-operation-present "delete")
          (ltu/is-operation-present "edit"))

      ;; check contents of resource
      (let [{:keys [name
                    description
                    properties
                    isSuperUser
                    state
                    lastOnline
                    activeSince
                    lastExecute
                    deleted] :as user} (-> session-admin
                                           (request abs-uri)
                                           (ltu/body->edn)
                                           :response
                                           :body)]
        (is (= name name-attr))
        (is (= description description-attr))
        (is (= properties properties-attr))
        (is (false? isSuperUser))
        (is (= user/initial-state state))
        (is (false? deleted))
        (is (= user/epoch lastOnline activeSince lastExecute)))

      ;; edit
      (let [body (-> session-admin
                     (request abs-uri)
                     (ltu/body->edn)
                     :response
                     :body)
            user-json (json/write-str (assoc body :isSuperUser true))]

        ;; anon users can NOT edit
        (-> session-anon
            (request abs-uri
                     :request-method :put
                     :body user-json)
            (ltu/is-status 403))

        ;; regular users can NOT set isSuperUser
        (-> session-user
            (request abs-uri
                     :request-method :put
                     :body user-json)
            (ltu/is-status 200))
        (is (false? (-> session-user
                        (request abs-uri)
                        (ltu/body->edn)
                        :response
                        :body
                        :isSuperUser)))

        ;; admin can set isSuperUser
        (-> session-admin
            (request abs-uri
                     :request-method :put
                     :body user-json)
            (ltu/body->edn)
            (ltu/is-status 200))
        (is (true? (-> session-admin
                       (request abs-uri)
                       (ltu/body->edn)
                       :response
                       :body
                       :isSuperUser))))

      ;; admin can delete resource
      (-> session-admin
          (request abs-uri
                   :request-method :delete)
          (ltu/body->edn)
          (ltu/is-status 200)))))
