(def +version+ "3.16-SNAPSHOT")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamPricingService-jar
  :version +version+
  :license {"commercial" "http://sixsq.com"}
  :edition "enterprise"

  :dependencies '[[org.clojure/clojure "1.9.0-alpha14"]
                  [sixsq/build-utils "0.1.4" :scope "test"]])

(require '[sixsq.build-fns :refer [merge-defaults
                                   sixsq-nexus-url
                                   lein-generate]])

(set-env!
  :repositories
  #(reduce conj % [["sixsq" {:url (sixsq-nexus-url)}]])
  :dependencies
  #(vec (concat %
                (merge-defaults
                 ['sixsq/default-deps (get-env :version)]
                 '[#_[org.clojure/clojurescript]

                   [org.clojure/core.async]
                   
                   [com.sixsq.slipstream/SlipStreamPricingLib-jar]
                   [com.sixsq.slipstream/SlipStreamPlacementLib-jar]

                   [compojure]
                   [aleph]
                   [environ]
                   [ring/ring-json]
                   [ring/ring-defaults]
                   [http-kit]

                   [adzerk/boot-test]
                   [tolitius/boot-check]
                   [pandeiro/boot-http]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[pandeiro.boot-http :refer [serve]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]])

(set-env!
  :source-paths #{"dev-resources" "test"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  uber {:exclude-scope #{"test"}
        :exclude       #{#".*/pom.xml"
                         #"META-INF/.*\.SF"
                         #"META-INF/.*\.DSA"
                         #"META-INF/.*\.RSA"}}
  serve {:handler 'sixsq.slipstream.pricing.service.server/app
         :reload true}
  watch {:verbose true})

(deftask run-tests
  "runs all tests and performs full compilation"
  []
  (comp
   (aot :all true)
   (test)))

(deftask build
  "build jar of service"
  []
  (comp
   (pom)
   (aot :all true)
   #_(aot :namespace #{'sixsq.slipstream.pricing.service.main})
   (jar)))

(deftask run
  "runs ring app and watches for changes"
  []
  (comp
   (watch)
   (pom)
   #_(aot :all true)
   (serve)))

(deftask mvn-test
         "run all tests of project"
         []
         (run-tests))

(deftask mvn-build
         "build full project through maven"
         []
         (comp
           (build)
           (install)
           (target)))

(deftask mvn-deploy
         "build full project through maven"
         []
         (comp
           (mvn-build)
           (push :repo "sixsq")))
