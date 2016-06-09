(ns sixsq.slipstream.prs.core
  "
  # Library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.data.json :as json]))

(defn call-prs
  [endpoint request]
  (let [components (:components request)
        connectors (:user-connectors request)]
    {:components (into []
                       (for [comp components]
                         (-> {}
                             (merge (select-keys comp [:module]))
                             (merge {:connectors (into [] (for [c connectors]
                                                            {:name     c
                                                             :price    0
                                                             :currency "none"}))}))))
     }))

(defn prs-place-and-rank
  "
  Input request map:
  {:components [{:module uri
                 :vm-size ''
                 :multiplicity #
                 :placement-policy ''} {}]
   :user-connectors [c1 c2]
  }

  Output
  {:components [{:module uri
                 :connectors [{:name c1 :price 0 :currency ''},
                              {:name c2 :price 0 :currency ''}]}]
  }
  "
  [endpoint prs-request]
  (if (some empty? (vals prs-request))
    {:components []}
    (call-prs endpoint prs-request))
  )

(defn merge-user-choices
  [components placement-params]
  components)

(defn build-prs-input
  [input]
  (-> {}
      (into {:components (merge-user-choices (-> input :module :components)
                                             (:placement-params input))})
      (into (select-keys input [:user-connectors]))))

(defn place-and-rank
  "Given the input map, calls PRS service and retuns the JSON returned by PRS.
   Input map
   {:module {:module uri
             :components [ {:module uri :vm-size '' }, ] }
    :placement-params { components: [ {:comp-uri uri :multiplicity # :policy string }, ] } ; map
    :prs-endpoint url ; str
    :user-connectors [c1 c2] ; vector
    }

  Output component
  {:components [{:module module/foo, :connectors [{:name c1, :price 0, :currency none},
                                                  {:name c2, :price 0, :currency none}]}]
  }
  Output app
  {:components [{:module module/foo, :connectors [{:name c1, :price 0, :currency none},
                                                  {:name c2, :price 0, :currency none}]}
                {:module module/bar, :connectors [{:name c1, :price 0, :currency none}]}]
   }
   "
  [input]
  (json/write-str
    (if (empty? input)
      {}
      (prs-place-and-rank (:prs-endpoint input) (build-prs-input input)))))


