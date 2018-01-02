(defproject
  com.sixsq.slipstream/SlipStreamPlacementLib-jar
  "3.32-SNAPSHOT"
  :license
  {"commercial" "http://sixsq.com"}
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha17"]
   [sixsq/build-utils "0.1.4" :scope "test"]
   [org.clojure/data.json "0.2.6"]
   [environ "1.1.0"]
   [superstring "2.1.0"]
   [com.sixsq.slipstream/SlipStreamPricingLib-jar "3.32-SNAPSHOT"]
   [com.sixsq.slipstream/SlipStreamClientAPI-jar "3.32-SNAPSHOT"]
   [adzerk/boot-test "1.2.0" :scope "test"]
   [adzerk/boot-reload "0.5.1" :scope "test"]
   [tolitius/boot-check "0.1.4" :scope "test"]]
  :source-paths
  ["test" "src"]
  :resource-paths ["resources"])