(ns com.sixsq.slipstream.ssclj.migrate.user-cred-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.migrate.user-cred :as t]))

(deftest exo-template
  (let [valid-template {:credentialTemplate {:key         "key"
                                             :secret      "secret"
                                             :domain-name "domain"
                                             :connector   "href"}}
        user "user"
        expected {:credentialTemplate {:key         "key"
                                       :secret      "secret"
                                       :domain-name "domain"
                                       :connector   "href"}
                  :user               "user"}]
    (is (= expected (t/check-exo-template valid-template user)))
    (is (= (assoc expected :user "newuser") (t/check-exo-template valid-template "newuser")))
    (is (= (assoc-in expected [:credentialTemplate :key] "newkey")
           (t/check-exo-template (assoc-in expected [:credentialTemplate :key] "newkey") user)))
    ;;empty key
    (is (nil? (t/check-exo-template (assoc-in expected [:credentialTemplate :key] "") user)))
    ;; mandatory keywords
    (doseq [k #{:key :secret :domain-name :connector}]
      (is (nil? (t/check-exo-template (update-in valid-template [:credentialTemplate] dissoc k) user))))))

(deftest otc-template
  (let [valid-template {:credentialTemplate {:key         "key"
                                             :secret      "secret"
                                             :domain-name "domain"
                                             :tenant-name "tenant"
                                             :connector   "href"}}
        user "user"
        expected {:credentialTemplate {:key         "key"
                                       :secret      "secret"
                                       :domain-name "domain"
                                       :tenant-name "tenant"
                                       :connector   "href"}
                  :user               "user"}]
    (is (= expected (t/check-otc-template valid-template user)))
    (is (= (assoc expected :user "newuser") (t/check-exo-template valid-template "newuser")))
    (is (= (assoc-in expected [:credentialTemplate :key] "newkey")
           (t/check-otc-template (assoc-in expected [:credentialTemplate :key] "newkey") user)))
    ;;empty key
    (is (nil? (t/check-otc-template (assoc-in expected [:credentialTemplate :key] "") user)))
    ;; mandatory keywords
    (doseq [k #{:key :secret :domain-name :tenant-name :connector}]
      (is (nil? (t/check-otc-template (update-in valid-template [:credentialTemplate] dissoc k) user))))))

(def user1 "user/user1")
(def user2 "user/user2")

(def coll {user1
           [{:ID                    103413,
             :VALUE                 "EXO0000000001",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                10,
             :INSTRUCTIONS          "On the Exoscale web interface you can find this information on <code>Account > Profile > API Keys</code>",
             :DESCRIPTION           "API Key",
             :CONTAINER_RESOURCEURI user1,
             :TYPE                  6,
             :READONLY              false,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.username"}
            {:ID                    103419,
             :VALUE                 "20",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                0,
             :INSTRUCTIONS          nil,
             :DESCRIPTION           "Number of VMs the user can start for this cloud",
             :CONTAINER_RESOURCEURI user1,
             :TYPE                  0,
             :READONLY              true,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.quota.vm"}
            {:ID                    103458,
             :VALUE                 "secret",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                20,
             :INSTRUCTIONS          "On the Exoscale web interface you can find this information on <code>Account > Profile > API Keys</code>",
             :DESCRIPTION           "Secret Key",
             :CONTAINER_RESOURCEURI user1,
             :TYPE                  2,
             :READONLY              false,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.password"}
            {:ID                    103487,
             :VALUE                 "",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                10,
             :INSTRUCTIONS          "If you use the DNS service of Exoscale you can set here the name of one of your domains and SlipStream will create a subdomain for each of your instances",
             :DESCRIPTION           "Name of your domain managed by Exoscale",
             :CONTAINER_RESOURCEURI user1,
             :TYPE                  0,
             :READONLY              false,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.domain.name"}],
           user2
           [{:ID                    76652,
             :VALUE                 "EXO0000000002",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                10,
             :INSTRUCTIONS          "On the Exoscale web interface you can find this information on <code>Account > Profile > API Keys</code>",
             :DESCRIPTION           "API Key",
             :CONTAINER_RESOURCEURI user2,
             :TYPE                  6,
             :READONLY              false,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.username"}
            {:ID                    76657,
             :VALUE                 "20",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                0,
             :INSTRUCTIONS          nil,
             :DESCRIPTION           "Number of VMs the user can start for this cloud",
             :CONTAINER_RESOURCEURI user2,
             :TYPE                  0,
             :READONLY              true,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.quota.vm"}
            {:ID                    73487,
             :VALUE                 "",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                10,
             :INSTRUCTIONS          "If you use the DNS service of Exoscale you can set here the name of one of your domains and SlipStream will create a subdomain for each of your instances",
             :DESCRIPTION           "Name of your domain managed by Exoscale",
             :CONTAINER_RESOURCEURI user2,
             :TYPE                  0,
             :READONLY              false,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.domain.name"}
            {:ID                    76693,
             :VALUE                 "secret",
             :MANDATORY             true,
             :CATEGORY              "exoscale-ch-gva",
             :ORDER_                20,
             :INSTRUCTIONS          "On the Exoscale web interface you can find this information on <code>Account > Profile > API Keys</code>",
             :DESCRIPTION           "Secret Key",
             :CONTAINER_RESOURCEURI user2,
             :TYPE                  2,
             :READONLY              false,
             :ENUMVALUES            nil,
             :NAME                  "exoscale-ch-gva.password"}]})


(deftest extracting-data
  (let [category-exo-gva "exoscale-ch-gva"
        category-otc "open-telekom-de1"
        wrong-category "wrong"
        expected1 {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale"
                                        :key         "EXO0000000001"
                                        :secret      "secret"
                                        :connector   "connector/exoscale-ch-gva"
                                        :domain-name ""}
                   :user               user1}
        expected2 {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale"
                                        :key         "EXO0000000002"
                                        :secret      "secret"
                                        :connector   "connector/exoscale-ch-gva"
                                        :domain-name ""}
                   :user               user2}]

    (are [expected category coll user] (= expected (t/extract-data category coll user))
                                       nil nil nil nil
                                       nil nil coll nil
                                       nil nil nil user1
                                       nil category-exo-gva coll nil
                                       nil nil coll user1
                                       nil category-exo-gva nil user1
                                       expected1 category-exo-gva coll user1
                                       expected2 category-exo-gva coll user2
                                       nil wrong-category coll user1
                                       nil category-otc coll user1)))



(deftest acl-merging
  (let [base-acl {:owner {:principal "ADMIN", :type "ROLE"},
                  :rules [{:type "ROLE", :principal "ADMIN", :right "ALL"}]}
        expect-user1 {:owner {:principal "ADMIN", :type "ROLE"},
                      :rules [{:type "ROLE", :principal "ADMIN", :right "ALL"}
                              {:type "USER", :principal "user1", :right "VIEW"}]}
        expect-2users (update-in expect-user1 [:rules] conj {:type "USER" :principal "user2" :right "VIEW"})]
    (are [expected arg] (= expected (t/merge-acl arg))
                        base-acl nil
                        base-acl []
                        base-acl {}
                        base-acl {:wrong "value"}
                        base-acl {:user "value"}
                        base-acl [nil]
                        base-acl ["foo"]
                        base-acl [{:wrong "value"}]
                        expect-user1 [{:user "user1"}]
                        expect-2users [{:user "user1"} {:user "user2"}]
                        expect-2users [{:user "user1" :extra "extra1"} {:user "user2" :extra "extra2"}])))


(deftest records-generations
  (let [base-template {:credentialTemplate {:acl {:owner {:principal "ADMIN", :type "ROLE"},
                                                  :rules [{:type "ROLE", :principal "ADMIN", :right "ALL"}]}}}
        simple-source (list [{:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale",
                                                   :key         "aKey",
                                                   :secret      "aSecret",
                                                   :connector   "connector/exoscale-ch-gva",
                                                   :domain-name ""},
                              :user               "user/user1"}])

        expect-simple (list {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale",
                                                  :key         "aKey",
                                                  :secret      "aSecret",
                                                  :connector   "connector/exoscale-ch-gva",
                                                  :domain-name "",
                                                  :acl         {:owner {:principal "ADMIN", :type "ROLE"},
                                                                :rules [{:type "ROLE", :principal "ADMIN", :right "ALL"}
                                                                        {:type "USER", :principal "user/user1", :right "VIEW"}]}}})
        multiplicity 42
        multi-source (map #(vector (conj {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale",
                                                               :key         "aKey",
                                                               :secret      "aSecret",
                                                               :connector   "connector/exoscale-ch-gva",
                                                               :domain-name ""}} {:user (str "user/user" %)})) (range multiplicity))
        base-expect {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale",
                                          :key         "aKey",
                                          :secret      "aSecret",
                                          :connector   "connector/exoscale-ch-gva",
                                          :domain-name "",
                                          :acl         {:owner {:principal "ADMIN", :type "ROLE"},
                                                        :rules [{:type "ROLE", :principal "ADMIN", :right "ALL"}]}}}
        add-user-rule (fn [x] (update-in base-expect [:credentialTemplate :acl :rules] conj {:type "USER" :principal (str "user/user" x) :right "VIEW"}))
        expect-multi (map add-user-rule (range multiplicity))]
    (are [arg] (thrown? AssertionError (t/generate-records arg))
               nil
               []
               {}
               "wrong"
               ["wrong"]
               [nil]
               [{}]
               [{:wrong "value"}]
               (list :wrong)
               (list [nil])
               (list [:test])
               (list ["value"]))
    (are [expected arg] (= expected (t/generate-records arg))
                        (list base-template) (list [{}])
                        (list base-template) (list [])
                        expect-simple simple-source
                        expect-multi multi-source)))






