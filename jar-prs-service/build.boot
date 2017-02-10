(def +version+ "3.21")

(set-env!
  :project 'com.sixsq.slipstream/SlipStreamPricingService-jar
  :version +version+
  :license {"commercial" "http://sixsq.com"}
  :edition "community"

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
                 '[[com.sixsq.slipstream/SlipStreamPlacementLib-jar]

                   [compojure]
                   [aleph]
                   [ring/ring-json]
                   [ring/ring-defaults]

                   [adzerk/boot-test]
                   [tolitius/boot-check]
                   [pandeiro/boot-http]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[pandeiro.boot-http :refer [serve]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]])

(set-env!
  :source-paths #{"dev-resources"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  serve {:handler 'sixsq.slipstream.prs.ring/handler
         :reload true}
  watch {:verbose true}
  push {:repo "sixsq"})

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
   (aot :namespace #{'sixsq.slipstream.prs.main})
   (jar)))

(deftask run
  "runs ring app and watches for changes"
  []
  (comp
   (watch)
   (pom)
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
           (if (= "true" (System/getenv "BOOT_PUSH"))
             (push)
             identity)))
