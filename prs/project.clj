(def +version+ "3.69")

(defproject com.sixsq.slipstream/SlipStreamPricingService-jar "3.69"

  :description "pricing service"

  :url "https://github.com/slipstream/SlipStreamServer"
  
  :license {:name "Apache 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0.txt"
            :distribution :repo}

  :plugins [[lein-parent "0.3.2"]]

  :parent-project {:coords  [sixsq/slipstream-parent "5.3.19"]
                   :inherit [:plugins
                             :min-lein-version
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
   [commons-io "2.5"]
   [compojure]
   [ring/ring-json]
   [ring/ring-defaults]]

  :profiles {:test {:source-paths   ["test"]
                    :resource-paths ["test-resources"]}})
