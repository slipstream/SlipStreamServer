(def +version+ "3.35")
 
(set-env!
  :project 'com.sixsq.slipstream/SlipStreamPlacementLib-jar
  :version +version+
  :license {"commercial" "http://sixsq.com"}
  :edition "community"
  :dependencies '[[org.clojure/clojure "1.9.0-alpha17"]
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
                 '[[org.clojure/data.json]
                   [environ]
                   [com.sixsq.slipstream/SlipStreamPricingLib-jar]
                   [com.sixsq.slipstream/SlipStreamClientAPI-jar]
                   [adzerk/boot-test]
                   [adzerk/boot-reload]
                   [tolitius/boot-check]]))))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.boot-reload :refer [reload]]
  '[tolitius.boot-check :refer [with-yagni with-eastwood with-kibit with-bikeshed]])

(set-env!
  :source-paths #{"test"}
  :resource-paths #{"src"})

(task-options!
  pom {:project (get-env :project)
       :version (get-env :version)}
  push {:repo "sixsq"})

(deftask run-tests
  "runs all tests and performs full compilation"
  []
  (comp
   (aot :all true)
   (test)))

(deftask build
  "build jar of library"
  []
  (comp
   (pom)
   (aot :all true)
   (jar)))

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
