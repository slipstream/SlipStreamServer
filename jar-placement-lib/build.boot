(def +version+ "3.27-SNAPSHOT")
 
(set-env!
  :project 'com.sixsq.slipstream/SlipStreamPlacementLib-jar
  :version +version+
  :license {"commercial" "http://sixsq.com"}
  :edition "community"
  :dependencies '[[org.clojure/clojure "1.9.0-alpha16"]
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

(defn- generate-lein-project-file! [& {:keys [keep-project] :or {keep-project true}}]
  (require 'clojure.java.io)
  (let [pfile ((resolve 'clojure.java.io/file) "project.clj")
        ; Only works when pom options are set using task-options!
        {:keys [project version]} (:task-options (meta #'boot.task.built-in/pom))
        prop #(when-let [x (get-env %2)] [%1 x])
        head (list* 'defproject (or project 'boot-project) (or version "0.0.0-SNAPSHOT")
               (concat
                 (prop :url :url)
                 (prop :license :license)
                 (prop :description :description)
                 [:dependencies (conj (get-env :dependencies)
                                      ['boot/core "2.6.0" :scope "compile"])
                  :repositories (get-env :repositories)
                  :source-paths (vec (concat (get-env :source-paths)
                                             (get-env :resource-paths)))]))
        proj (pp-str head)]
      (if-not keep-project (.deleteOnExit pfile))
      (spit pfile proj)))

(deftask lein
  "Generate a leiningen `project.clj` file.
   This task generates a leiningen `project.clj` file based on the boot
   environment configuration, including project name and version (generated
   if not present), dependencies, and source paths. Additional keys may be added
   to the generated `project.clj` file by specifying a `:lein` key in the boot
   environment whose value is a map of keys-value pairs to add to `project.clj`."
 []
 (with-pass-thru fs (generate-lein-project-file! :keep-project true)))