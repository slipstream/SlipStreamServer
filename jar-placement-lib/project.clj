(def +version+ "3.54-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamPlacementLib-jar "3.54-SNAPSHOT"

  :description "placement library"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.3.8"]
                   :inherit [:plugins
                             :min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :resource-paths ["resources"]

  :test-paths ["test"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/data.json]
   [environ]
   [com.sixsq.slipstream/SlipStreamPricingLib-jar ~+version+]
   [com.sixsq.slipstream/SlipStreamClojureAPI-cimi ~+version+]])

