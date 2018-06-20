(ns com.sixsq.slipstream.ssclj.resources.spec.module-application-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.connector :as t]  ;; FIXME: Change to module-version when available.
    [com.sixsq.slipstream.ssclj.resources.spec.module-application :as module-app]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id          (str t/resource-url "/connector-uuid")
              :resourceURI t/resource-uri
              :created     timestamp
              :updated     timestamp
              :acl         valid-acl

              :nodes       [{:component    {:href "module/a-b"}
                             :multiplicity 1}
                            {:component               {:href "module/c-d"}
                             :multiplicity            2
                             :maxProvisioningFailures 1}
                            {:component               {:href "module/e-f"}
                             :multiplicity            20
                             :maxProvisioningFailures 10
                             :parameterMappings       {:param.1-2 {:mapped true, :value "param.2-3"}
                                                       :param.3-4 {:mapped false, :value "default"}}}]}]

    (is (s/valid? ::module-app/module-application root))
    (is (false? (s/valid? ::module-app/module-application (assoc root :badKey "badValue"))))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :nodes}]
      (is (false? (s/valid? ::module-app/module-application (dissoc root k)))))

    ;; optional attributes
    (doseq [k #{:parameterMappings}]
      (is (true? (s/valid? ::module-app/module-application (dissoc root k)))))))
