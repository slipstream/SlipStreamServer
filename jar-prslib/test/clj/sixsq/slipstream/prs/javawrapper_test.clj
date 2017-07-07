(ns sixsq.slipstream.prs.javawrapper-test
  (:require
    [clojure.test :refer :all]
    [sixsq.slipstream.prs.javawrapper :refer :all]
    [com.sixsq.slipstream.db.serializers.utils :as su]
    [com.sixsq.slipstream.db.serializers.service-config-impl :as sci]
    )
  (:import [java.util HashMap ArrayList HashSet]
           [com.sixsq.slipstream.persistence ImageModule DeploymentModule Node ModuleParameter ParameterCategory]))

;; Fixtures.
(defn fixture-start-es-db
  [f]
  (su/test-db-client-and-crud-impl)
  ;; initialize resource (including possible connectors on the classpath).
  (su/initialize)
  (sci/db-add-default-config)
  (f))
(use-fixtures :once fixture-start-es-db)

(def cm {"a" "1" "b" "2" "c" "3"})
(def m (HashMap. cm))

(def cs #{"a" "A" "b" "B"})
(def s (HashSet. cs))

(def cl ["one" "two" "three"])
(def l (ArrayList. cl))

(def nested-map (HashMap. {"map" m "set" s "list" l}))

(deftest test-keyword-name
  (are [x y] (= x y)
             "a/b/c/1" (keyword-name (keyword "a/b/c/1"))
             "a" (keyword-name (keyword "a"))
             "a#$?!~" (keyword-name (keyword "a#$?!~"))
             "a" (keyword-name "a")
             "" (keyword-name nil)
             "3" (keyword-name 3)
  ))

(deftest test-java-to-clj
  (is (= cm (java->clj m)))
  (is (= cs (java->clj s)))
  (is (= cl (java->clj l)))

  (is (map? (java->clj nested-map)))
  (is (= cm (get (java->clj nested-map) "map")))
  (is (= cs (get (java->clj nested-map) "set")))
  (is (= cl (get (java->clj nested-map) "list"))))

(deftest test-placement->map
  (let [image1 (doto
                 (ImageModule. "component1")
                 (.setIsBase true)
                 (.setParameter (ModuleParameter. "cpu.nb" "2" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "ram.GB" "8" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "disk.GB" "50" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "c1.instance.type" "medium" "" ParameterCategory/Cloud))
                 (.setPlatform "centos")
                 )
        image2 (doto
                 (ImageModule. "component2")
                 (.setIsBase true)
                 (.setParameter (ModuleParameter. "cpu.nb" "1" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "ram.GB" "4" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "disk.GB" "10" "" ParameterCategory/Cloud))
                 (.setPlatform "windows")
                 (.setPlacementPolicy "schema-org:location='de'"))
        image3 (doto
                 (ImageModule. "component3")
                 (.setIsBase true)
                 (.setPlatform "ubuntu")
                 (.setParameter (ModuleParameter. "c1.instance.type" "large" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "c1.disk" "50G" "" ParameterCategory/Cloud)))
        app (doto (DeploymentModule. "application")
                  (.setNode (Node. "node1" image1))
                  (.setNode (Node. "node2" image2)))
        app-map (placement->map {:module          app
                                 :user-connectors ["c5" "c6"]})]

    (is (= {:components      [{:module           "module/component1"
                               :placement-policy nil
                               :cpu.nb           "2"
                               :ram.GB           "8"
                               :disk.GB          "50"
                               :operating-system "linux"
                               :connector-instance-types {"c1" {:instance.type "medium"} "c2" {}}}
                              {:node             "node-orchestrator"
                               :module           "module-orchestrator"
                               :cpu.nb           nil
                               :ram.GB           nil
                               :disk.GB          nil
                               :operating-system "linux"
                               :placement-policy nil
                               :connector-instance-types
                                                 {"c1" {:instance.type nil, :cpu 0, :ram 0, :disk 0},
                                                  "c2" {:instance.type nil, :cpu 0, :ram 0, :disk 0}}
                               }]

            :user-connectors ["c1" "c2"]}
           (placement->map {:module          image1
                            :user-connectors ["c1" "c2"]})))

       (is (= {:components      [{:module           "module/component3"
                                  :placement-policy nil
                                  :cpu.nb           nil
                                  :ram.GB           nil
                                  :disk.GB          nil
                                  :operating-system "linux"
                                  :connector-instance-types {"c1" {:instance.type "large" :disk "50"}}}
                                 {:node             "node-orchestrator"
                                  :module           "module-orchestrator"
                                  :cpu.nb           nil
                                  :ram.GB           nil
                                  :disk.GB          nil
                                  :operating-system "linux"
                                  :placement-policy nil
                                  :connector-instance-types
                                                    {"c1" {:instance.type nil, :cpu 0, :ram 0, :disk 0}}
                                  }]

               :user-connectors ["c1"]}
              (placement->map {:module          image3
                               :user-connectors ["c1"]})))

    (is (= {:components      [{:module           "module/component2"
                               :placement-policy "schema-org:location='de'"
                               :cpu.nb           "1"
                               :ram.GB           "4"
                               :disk.GB          "10"
                               :operating-system "windows"
                               :connector-instance-types {"c3" {}}}
                              {:node             "node-orchestrator"
                               :module           "module-orchestrator"
                               :cpu.nb           nil
                               :ram.GB           nil
                               :disk.GB          nil
                               :operating-system "linux"
                               :placement-policy nil
                               :connector-instance-types
                                                 {"c3" {:instance.type nil, :cpu 0, :ram 0, :disk 0}}
                               }]

            :user-connectors ["c3"]}
           (placement->map {:module          image2
                            :user-connectors ["c3"]})))

    (is (= ["c5" "c6"] (:user-connectors app-map)))

    (is (= #{{:module           "module/component2"
              :node             "node2"
              :cpu.nb           "1"
              :ram.GB           "4"
              :disk.GB          "10"
              :operating-system "windows"
              :placement-policy "schema-org:location='de'"
              :connector-instance-types {"c5" {} "c6" {}}}
             {:module           "module/component1"
              :node             "node1"
              :cpu.nb           "2"
              :ram.GB           "8"
              :disk.GB          "50"
              :operating-system "linux"
              :placement-policy nil
              :connector-instance-types {"c5" {} "c6" {}}}
             {:node             "node-orchestrator"
              :module           "module-orchestrator"
              :cpu.nb           nil
              :ram.GB           nil
              :disk.GB          nil
              :operating-system "linux"
              :placement-policy nil
              :connector-instance-types
                                {"c5" {:instance.type nil, :cpu 0, :ram 0, :disk 0}
                                 "c6" {:instance.type nil, :cpu 0, :ram 0, :disk 0}}
              }}

           (set (:components app-map))))))

(deftest test-try-extract-digit
  (are [x y] (= x y)
             "1" (try-extract-digit "1")
             "10" (try-extract-digit "10G")  ;exoscale disk value format
             "0.5" (try-extract-digit "0.5")
             "0.5" (try-extract-digit 0.5)
             nil (try-extract-digit nil)
             nil (try-extract-digit "ABC")
             nil (try-extract-digit "")))
