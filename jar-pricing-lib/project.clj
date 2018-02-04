(def +version+ "3.45")

(defproject com.sixsq.slipstream/SlipStreamPricingLib-jar "3.45"

  :description "pricing library"

  :url "https://github.com/slipstream/SlipStreamServer"

  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.45"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :test-paths ["test"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]])
