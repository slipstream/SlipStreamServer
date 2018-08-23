(ns com.sixsq.slipstream.ssclj.resources.spec.module-component-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer [are deftest is]]
    [com.sixsq.slipstream.ssclj.resources.connector :as t]  ;; FIXME: Change to module-version when available.
    [com.sixsq.slipstream.ssclj.resources.spec.module-component :as module-component]
    [com.sixsq.slipstream.ssclj.resources.spec.spec-test-utils :as stu]))


(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "ANON"
                         :type      "ROLE"
                         :right     "VIEW"}]})


(deftest test-schema-check
  (let [timestamp "1964-08-25T10:00:00.0Z"
        root {:id               (str t/resource-url "/connector-uuid")
              :resourceURI      t/resource-uri
              :created          timestamp
              :updated          timestamp
              :acl              valid-acl

              :parent           {:href "module/my-parent-module"}
              :cpu              2
              :ram              2048
              :disk             100
              :volatileDisk     500
              :networkType      "public"

              :inputParameters  [{:parameter "iparam-1" :description "desc2" :value "100"}
                                 {:parameter "iparam-2" :description "desc2"}
                                 {:parameter "iparam-3"}]

              :outputParameters [{:parameter "iparam-1" :description "desc2" :value "100"}
                                 {:parameter "iparam-2" :description "desc2"}
                                 {:parameter "iparam-3"}]

              :targets          {:preinstall  "preinstall"
                                 :packages    ["emacs-nox" "vim"]
                                 :postinstall "postinstall"
                                 :deployment  "deployment"
                                 :reporting   "reporting"
                                 :onVmAdd     "onVmAdd"
                                 :onVmRemove  "onVmRemove"
                                 :prescale    "prescale"
                                 :postscale   "postscale"}
              :author           "someone"
              :commit           "wip"}]

    (stu/is-valid ::module-component/module-component root)
    (stu/is-invalid ::module-component/module-component (assoc root :badKey "badValue"))

    ;; required attributes
    (doseq [k #{:id :resourceURI :created :updated :acl :networkType :outputParameters :author}]
      (stu/is-invalid ::module-component/module-component (dissoc root k)))

    ;; optional attributes
    (doseq [k #{:cpu :ram :disk :volatileDisk :targets :inputParameters :commit}]
      (stu/is-valid ::module-component/module-component (dissoc root k)))))
