(def +version+ "3.43-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
  "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamCljResourcesTests-jar
  "3.43-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.43-SNAPSHOT"]
                   :inherit [:min-lein-version :managed-dependencies :repositories :deploy-repositories]}

  :source-paths ["../jar/test"]

  :pom-location "target/"

  :jar-exclusions [#".*"]

  :jar-inclusions [#"lifecycle_test_utils\.clj"
                   #"connector_test_utils\.clj"]

  :dependencies [[org.apache.curator/curator-test :scope "compile"]
                 [com.cemerick/url]
                 [com.sixsq.slipstream/slipstream-ring-container]])