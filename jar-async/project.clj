(defproject com.sixsq.slipstream/async "2.9.0-SNAPSHOT"
  :description    "Clojure Async services"
  :url            "http://sixsq.com"
  :license        {:name "Apache License, Version 2.0"
                   :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :source-paths   ["src"]
  :test-paths     ["test"]
  :resource-paths ["resources"]

  :dependencies   [[org.clojure/clojure                  "1.8.0"]
                   [org.clojure/core.async               "0.2.374"]
                   [com.sixsq.slipstream/SlipStreamAsync "3.3-SNAPSHOT"]]

  :plugins        [[lein-expectations                    "0.0.7"]
                   [lein-autoexpect                      "1.4.2"]
                   [com.jakemccrary/lein-test-refresh    "0.5.5"]])

