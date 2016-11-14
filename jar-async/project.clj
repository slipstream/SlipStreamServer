(defproject
  com.sixsq.slipstream/SlipStreamAsync
  "3.12-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies
  [[org.clojure/clojure "1.8.0"]
   [sixsq/build-utils "0.1.4" :scope "test"]
   [org.clojure/clojure "1.8.0"]
   [org.clojure/core.async
    "0.2.385"
    :exclusions
    [org.clojure/tools.reader]]
   [com.sixsq.slipstream/SlipStreamPersistence
    "3.12-SNAPSHOT"
    :scope
    "test"]
   [com.sixsq.slipstream/SlipStreamConnector
    "3.12-SNAPSHOT"
    :scope
    "test"]
   [com.sixsq.slipstream/SlipStreamDbSerializers-jar
    "3.12-SNAPSHOT"
    :scope
    "test"]
   [adzerk/boot-test "1.1.2" :scope "test"]
   [adzerk/boot-reload "0.4.12" :scope "test"]
   [tolitius/boot-check "0.1.3" :scope "test"]
   [boot-codox "0.9.6" :scope "test"]]
  :source-paths
  ["test" "src"])