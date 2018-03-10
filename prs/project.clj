(def +version+ "3.48-SNAPSHOT")

(defproject com.sixsq.slipstream/SlipStreamPricingService-jar "3.48-SNAPSHOT"

  :description "pricing service"

  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.0.2"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src"]

  :pom-location "target/"

  :aot :all

  :dependencies
  [[org.clojure/clojure]
   [org.clojure/tools.nrepl]
   [cheshire] ;; newer version needed
   [com.sixsq.slipstream/SlipStreamPlacementLib-jar ~+version+]
   [com.sixsq.slipstream/token ~+version+]
   [compojure]
   [ring/ring-json]
   [ring/ring-defaults]]

  :profiles {:test {:source-paths   ["test"]
                    :resource-paths ["test-resources"]}})
