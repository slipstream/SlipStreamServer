(ns com.sixsq.slipstream.auth.external-test
  (:require
    [clojure.string :as str]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.auth.external :refer :all]
    [com.sixsq.slipstream.auth.test-helper :as th]
    [com.sixsq.slipstream.auth.utils.db :as db]
    [com.sixsq.slipstream.ssclj.resources.lifecycle-test-utils :as ltu]))


(use-fixtures :each ltu/with-test-server-fixture)


(deftest match-already-mapped-legacy
  (let [get-db-user #(-> (db/get-all-users) first (dissoc :updated))
        user-info {:username     "joe"
                   :password     "secret"
                   :githublogin  "st"
                   :emailAddress "st@sixsq.com"
                   :state        "ACTIVE"}
        _ (th/add-user-for-test! user-info)
        user (get-db-user)]

    (match-external-user! :github "st" "st@sixsq.com")
    (is (= user (get-db-user)))))


(deftest match-already-mapped
  (let [get-db-user #(-> (db/get-all-users) first (dissoc :updated))
        user-info {:username         "joe"
                   :password         "secret"
                   :externalIdentity ["github:st"]
                   :emailAddress     "st@sixsq.com"
                   :state            "ACTIVE"}
        _ (th/add-user-for-test! user-info)
        user (get-db-user)]

    (match-external-user! :github "st" "st@sixsq.com")
    (is (= user (get-db-user)))))


(deftest match-existing-external-user-does-not-create
  (is (= [] (db/get-all-users)))

  (match-existing-external-user :github "st" "st@sixsq.com")
  (is (empty? (db/get-all-users))))


(deftest match-existing-user-does-not-match-email
  (th/add-user-for-test! {:username     "joe"
                          :password     "secret"
                          :emailAddress "st@sixsq.com"
                          :state        "ACTIVE"})
  (let [users-before-match (db/get-all-users)]
    (is (= 1 (count users-before-match)))
    (is (nil? (:githublogin (first users-before-match)))))

  (match-existing-external-user :github "st" "st@sixsq.com")

  (let [users-after-match (db/get-all-users)]
    (is (= 1 (count users-after-match)))
    (is (nil? (:githublogin (first users-after-match))))))


(deftest match-already-mapped
  (let [get-db-user #(-> (db/get-all-users) first (dissoc :updated))
        user-info {:username     "joe"
                   :password     "secret"
                   :githublogin  "st"
                   :emailAddress "st@sixsq.com"
                   :state        "ACTIVE"}
        _ (th/add-user-for-test! user-info)
        user (get-db-user)]

    ;; explicitly mapped; should be OK
    (match-existing-external-user :github "st" "st@sixsq.com")
    (is (= user (get-db-user)))))


(defn get-identity
  [authn-method external-record]
  (let [externalIdentity (->> (create-user-when-missing! authn-method external-record)
                              (db/get-user)
                              :externalIdentity
                              (filter #(str/starts-with? % (name authn-method)))
                              first
                              )]
    (or externalIdentity
        ;;use fallback
        (->> (create-user-when-missing! authn-method external-record)
             (db/get-user)
             :username))))


(deftest oidc-user-names
  (let [users (db/get-active-users)
        authn-methods #{:github :oidc :others}]

    (is (zero? (count users)))

    (doseq [authn-method authn-methods]
      (is (= (str (name authn-method) ":" "a_b_c_d_e") (get-identity authn-method {:external-login    "a/b!c@d#e"
                                                                                   :external-email    "bad-address@example.com"
                                                                                   :fail-on-existing? false}))))
    (let [users (db/get-active-users)]
      (is (= (count authn-methods) (count users))))


    (doseq [authn-method authn-methods]
      (is (= (str (name authn-method) ":" "a_b_c_d_e") (get-identity authn-method {:external-login    "a/b!c@d#e"
                                                                                   :external-email    "bad-address@example.com"
                                                                                   :fail-on-existing? false}))))
    (let [users (db/get-active-users)]
      (is (= (count authn-methods) (count users))))


    (doseq [authn-method authn-methods]
      (is (= (str (name authn-method) ":" "A_B_C_D_E") (get-identity authn-method {:external-login    "A/B!C@D#E"
                                                                                   :external-email    "bad-address@example.com"
                                                                                   :fail-on-existing? false}))))
    (let [users (db/get-active-users)]
      (is (= (* 2 (count authn-methods)) (count users))))))


(deftest check-create-user-when-missing!
  (let [users (db/get-active-users)
        authn-methods #{:oidc :github :other}]

    (is (zero? (count users)))
    (th/add-user-for-test! {:username     "not-missing"
                            :password     "secret"
                            :emailAddress "not-missing@example.com"
                            :state        "ACTIVE"})
    (let [users (db/get-all-users)]
      (is (= 1 (count users))))

    (doseq [authn-method authn-methods]
      (is (= "not-missing" (get-identity authn-method {:external-login    "not-missing"
                                                       :external-email    "bad-address@example.com"
                                                       :fail-on-existing? false}))))

    (let [users (db/get-all-users)]
      (is (= 1 (count users))))


    (doseq [authn-method authn-methods]
      (is (= (str (name authn-method) ":" "missing)" (get-identity authn-method {:external-login    "missing"
                                                                                 :external-email    "ok@example.com"
                                                                                 :fail-on-existing? false})))))

    (let [users (db/get-all-users)
          user-params (db/get-all-user-params)]
      (is (= (inc (count authn-methods)) (count users)))
      (is (= (count authn-methods) (count user-params))))


    (doseq [authn-method authn-methods]
      (is (= (str (name authn-method) ":" "deleted)" (get-identity authn-method {:external-login    "deleted"
                                                                                 :external-email    "ok@example.com"
                                                                                 :state             "DELETED"
                                                                                 :fail-on-existing? false})))))

    (let [users (db/get-all-users)
          user-params (db/get-all-user-params)]
      (is (= (inc (* 2 (count authn-methods))) (count users)))
      (is (= (* 2 (count authn-methods)) (count user-params))))


    (doseq [authn-method authn-methods]
      (is (= (str (name authn-method) ":" "deleted)" (get-identity authn-method {:external-login    "deleted"
                                                                                 :external-email    "ok@example.com"
                                                                                 :state             "DELETED"
                                                                                 :fail-on-existing? true})))))
    (let [users (db/get-all-users)
          user-params (db/get-all-user-params)]
      (is (= (inc (* 2 (count authn-methods))) (count users)))
      (is (= (* 2 (count authn-methods)) (count user-params))))))


(deftest test-new-user-with-params!
  (let [users (db/get-active-users)]
    (is (zero? (count users)))

    (is (= (str (name :github) ":" "missing") (get-identity :github {:external-login "missing"
                                                                     :external-email "ok@example.com"})))

    (let [user-params (db/get-all-user-params)]
      (is (= (str (name :github) ":" "missing") (-> user-params
                                                    first
                                                    (get-in [:acl :owner :principal])
                                                    (db/get-user)
                                                    :externalIdentity
                                                    first)))

      (is (= 1 (count user-params))))))


(deftest test-sanitize-login-name
  (is (= "st" (sanitize-login-name "st")))
  (is (= "Paul_Newman" (sanitize-login-name "Paul Newman")))
  (is (= "abc-def-123" (sanitize-login-name "abc-def-123")))
  (is (= "a_b_c_d_e" (sanitize-login-name "a/b!c@d#e"))))
