(ns com.sixsq.slipstream.ssclj.migrate.user-cred-test
  (:require
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.migrate.user-cred :as t]))

(def user1 "user/user1")
(def user2 "user/user2")

(def collect {user1
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
                                        :domain-name ""
                                        :acl         {:owner {:principal "user1"
                                                              :type      "USER"}
                                                      :rules [{:principal "ADMIN"
                                                               :right     "ALL"
                                                               :type      "ROLE"}
                                                              {:principal "user1"
                                                               :right     "MODIFY"
                                                               :type      "USER"}]}
                                        }
                   :name               "exoscale-ch-gva"
                   }
        expected2 {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale"
                                        :key         "EXO0000000002"
                                        :secret      "secret"
                                        :connector   "connector/exoscale-ch-gva"
                                        :domain-name ""
                                        :acl         {:owner {:principal "user2"
                                                              :type      "USER"}
                                                      :rules [{:principal "ADMIN"
                                                               :right     "ALL"
                                                               :type      "ROLE"}
                                                              {:principal "user2"
                                                               :right     "MODIFY"
                                                               :type      "USER"}]}}
                   :name               "exoscale-ch-gva"}]

    (are [expected category coll user] (= expected (t/extract-data category coll user))
                                       nil nil nil nil
                                       nil nil collect nil
                                       nil nil nil user1
                                       nil category-exo-gva collect nil
                                       nil nil collect user1
                                       nil category-exo-gva nil user1
                                       expected1 category-exo-gva collect user1
                                       expected2 category-exo-gva collect user2
                                       nil wrong-category collect user1
                                       nil category-otc collect user1)))

(deftest mapped-connectors
  (are [expect-fn arg] (expect-fn (t/mapped arg))
                       nil? nil
                       nil? :wrong
                       nil? "wrong"
                       #(contains? % :template-name) "exoscale-ch-gva"))

(deftest template-validation
  ;; invalid scenario
  (are [tpl ks] (not (t/valid-template? tpl ks))
                nil nil
                nil :k
                nil [:k1 :k2]
                nil {}
                {} nil
                :t nil
                ;;:t :t
                {:k :v} nil
                ;;{:k :v} :k
                {:k :v} [:k]
                {:credentialTemplate {:k1 :v1
                                      :k2 :v2
                                      }} [:k1 :k2 :k3]
                {:credentialTemplate {:href      "credential-template/store-cloud-cred-nuvlabox",
                                      :key       "",
                                      :secret    "",
                                      :connector "connector/nuvlabox-carl-cori"}} t/keys-cred-nuvlabox
                {:credentialTemplate {:key "key" :tenant-name ""}} [:tenant-name])

  ;;valid scenario
  (are [tpl ks] (t/valid-template? tpl ks)
                {:credentialTemplate {:k :v :key "key" :secret "secret"}} [:k]
                {:credentialTemplate {:key    "key"
                                      :secret "secret"
                                      :k1     :v1
                                      :k2     :v2
                                      }} [:k1 :k2]
                {:credentialTemplate {:href        "credential-template/store-cloud-cred-exoscale",
                                      :key         "EXO0000000001",
                                      :secret      "secret",
                                      :domain-name "",
                                      :connector   "connector/exoscale-ch-gva"}} [:href :key :secret :domain-name :connector]))

(deftest mappings-size
  (is (= (count t/mappings) (reduce + [(count t/mappings-nuvlabox)
                                       (count t/mappings-stratuslabiter)
                                       (count t/mappings-otc)
                                       (count t/mappings-openstack)
                                       (count t/mappings-opennebula)
                                       (count t/mappings-exoscale)
                                       (count t/mappings-ec2)]))))
