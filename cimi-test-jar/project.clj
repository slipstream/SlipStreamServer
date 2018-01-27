(def +version+ "3.45-SNAPSHOT")

(defproject
  com.sixsq.slipstream/SlipStreamCljResourcesTests-jar
  "3.45-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :dependencies [[com.sixsq.slipstream/SlipStreamDbBinding-jar]
                 [com.sixsq.slipstream/SlipStreamDbTesting-jar :scope "compile"]
                 [org.apache.curator/curator-test :scope "compile"]
                 [peridot "0.5.0" :scope "compile"]
                 [org.clojure/data.json]
                 [compojure]
                 [com.cemerick/url]
                 [com.sixsq.slipstream/slipstream-ring-container]])
