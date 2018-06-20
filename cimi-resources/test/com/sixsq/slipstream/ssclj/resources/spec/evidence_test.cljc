(ns com.sixsq.slipstream.ssclj.resources.spec.evidence-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.test :refer :all]
    [com.sixsq.slipstream.ssclj.resources.evidence :as ev]))

(def valid-acl {:owner {:principal "ADMIN"
                        :type      "ROLE"}
                :rules [{:principal "USER"
                         :type      "ROLE"
                         :right     "VIEW"}]})

(def timestamp "1964-08-25T10:00:00.0Z")

(def evid {:id            (str ev/resource-url "/uuid")
                  :resourceURI   ev/resource-uri
                  :created       timestamp
                  :updated       timestamp
                  :acl           valid-acl

                  :name          "name"
                  :description   "short description",
                  :properties    {:a "one",
                                  :b "two"}

                  ; :credentials   [{:href "credential/e3db10f4-ad81-4b3e-8c04-4994450da9e3"}]

                  :endTime        timestamp
                  :startTime      timestamp
                  :planID         "b12345"
                  :passed         true
                  :class          "className"

                  :className:random 3147.12

                  :other         "value"})


(deftest check-ServiceInfo

  (are [expect-fn arg] (expect-fn (s/valid? :cimi/evidence arg))
                       true? evid
                       false? (dissoc evid :created)
                       false? (dissoc evid :updated)
                       false? (dissoc evid :acl)
                      ;  false? (dissoc evid :credentials)
                       false? (dissoc evid :passed)
                       false? (dissoc evid :planID)
                       false? (dissoc evid :endTime)
                       false? (dissoc evid :startTime)
                       false? (dissoc evid :class)
                       true? (dissoc evid :className:random)
                       true? (dissoc evid :other)))
