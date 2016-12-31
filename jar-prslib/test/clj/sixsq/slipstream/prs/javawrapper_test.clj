(ns sixsq.slipstream.prs.javawrapper-test
  (:require
    [clojure.test :refer :all]
    [sixsq.slipstream.prs.javawrapper :refer :all])
  (:import [java.util HashMap ArrayList HashSet]
           [com.sixsq.slipstream.persistence ImageModule DeploymentModule Node ModuleParameter ParameterCategory]))

(def cm {"a" "1" "b" "2" "c" "3"})
(def m (HashMap. cm))

(def cs #{"a" "A" "b" "B"})
(def s (HashSet. cs))

(def cl ["one" "two" "three"])
(def l (ArrayList. cl))

(def nested-map (HashMap. {"map" m "set" s "list" l}))

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
                 (.setParameter (ModuleParameter. "cpu.nb" "2" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "ram.GB" "8" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "disk.GB" "50" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "c1.instance.type" "medium" "" ParameterCategory/Cloud))
                 )
        image2 (doto
                 (ImageModule. "component2")
                 (.setParameter (ModuleParameter. "cpu.nb" "1" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "ram.GB" "4" "" ParameterCategory/Cloud))
                 (.setParameter (ModuleParameter. "disk.GB" "10" "" ParameterCategory/Cloud))
                 (.setPlacementPolicy "schema-org:location='de'"))
        app (doto (DeploymentModule. "application")
                  (.setNode (Node. "node1" image1))
                  (.setNode (Node. "node2" image2)))
        app-map (placement->map {:module          app
                                 :user-connectors ["c5" "c6"]})]

    (is (= {:prs-endpoint    "http://localhost:8203/filter-rank"
            :components      [{:module           "module/component1"
                               :placement-policy nil
                               :cpu.nb           "2"
                               :ram.GB           "8"
                               :disk.GB          "50"
                               :connector-instance-types {"c1" "medium" "c2" nil}}]

            :user-connectors ["c1" "c2"]}
           (placement->map {:prs-endpoint    "http://localhost:8203/filter-rank"
                            :module          image1
                            :user-connectors ["c1" "c2"]})))

    (is (= {:components      [{:module           "module/component2"
                               :placement-policy "schema-org:location='de'"
                               :cpu.nb           "1"
                               :ram.GB           "4"
                               :disk.GB          "10"
                               :connector-instance-types {"c3" nil}}]
            :user-connectors ["c3"]}
           (placement->map {:module          image2
                            :user-connectors ["c3"]})))

    (is (= ["c5" "c6"] (:user-connectors app-map)))

    (is (= #{{:module           "module/component2"
              :node             "node2"
              :cpu.nb           "1"
              :ram.GB           "4"
              :disk.GB          "10"
              :placement-policy "schema-org:location='de'"
              :connector-instance-types {"c5" nil "c6" nil}}
             {:module           "module/component1"
              :node             "node1"
              :cpu.nb           "2"
              :ram.GB           "8"
              :disk.GB          "50"
              :placement-policy nil
              :connector-instance-types {"c5" nil "c6" nil}}}

           (set (:components app-map))))))
