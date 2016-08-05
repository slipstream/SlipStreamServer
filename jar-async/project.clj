(defproject com.sixsq.slipstream/async "3.10-SNAPSHOT"
  :description "Clojure Async services"
  :url "http://sixsq.com"
  :license {:name "Apache License, Version 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :source-paths ["src"]
  :test-paths ["test"]
  :resource-paths ["resources"]

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.385"]
                 ;[com.sixsq.slipstream/auth "3.10-SNAPSHOT"]
                 [com.sixsq.slipstream/SlipStreamPersistence "3.10-SNAPSHOT"]
                 [com.sixsq.slipstream/SlipStreamConnector "3.10-SNAPSHOT"]
                 ]
  )

