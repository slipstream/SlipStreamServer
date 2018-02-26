(def +version+ "3.46")

(defproject com.sixsq.slipstream/SlipStreamPricingService-jar "3.46"

  :description "pricing service"

  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [com.sixsq.slipstream/parent "3.46"]
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
   [com.sixsq.slipstream/SlipStreamPlacementLib-jar]
   [com.sixsq.slipstream/token]
   [compojure]
   [ring/ring-json]
   [ring/ring-defaults]]

  :profiles {:test {:source-paths   ["test"]
                    :resource-paths ["test-resources"]}})
