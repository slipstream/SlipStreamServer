(defproject
  com.sixsq.slipstream/SlipStreamAsync
  "3.23-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha14"]
   [sixsq/build-utils "0.1.4" :scope "test"]
   [org.clojure/clojure "1.9.0-alpha14"]
   [org.clojure/core.async
    "0.2.395"
    :exclusions
    [org.clojure/tools.reader]]
   [com.sixsq.slipstream/SlipStreamPersistence
    "3.23-SNAPSHOT"
    :scope
    "test"]
   [com.sixsq.slipstream/SlipStreamConnector
    "3.23-SNAPSHOT"
    :scope
    "test"]
   [com.sixsq.slipstream/SlipStreamDbSerializers-jar
    "3.23-SNAPSHOT"
    :scope
    "test"]
   [adzerk/boot-test "1.2.0" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [tolitius/boot-check "0.1.4" :scope "test"]
   [boot-codox "0.10.3" :scope "test"]]
  :source-paths
  ["test" "src"])