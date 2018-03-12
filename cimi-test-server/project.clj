(def +version+ "3.48-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamCljResourcesTestServer-jar "3.48-SNAPSHOT"

  :description "complete test server"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.1.0"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :aot [com.sixsq.slipstream.ssclj.app.CIMITestServer]

  :dependencies [[org.apache.curator/curator-test :scope "compile"]
                 [com.sixsq.slipstream/SlipStreamCljResources-jar ~+version+]
                 [com.sixsq.slipstream/slipstream-ring-container ~+version+]])
