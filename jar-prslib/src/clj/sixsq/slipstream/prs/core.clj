(ns sixsq.slipstream.prs.core
  "
  # Library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.data.json :as json]
    [sixsq.slipstream.client.api.utils.http-sync :as http]
    [clojure.tools.logging :as log]))

(defn call-prs
  [body endpoint & [http-params]]
  (:body (http/put endpoint (merge {:body body} http-params))))


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
  (if (or (empty? prs-request) (some empty? (vals prs-request)))
    {:components []}
    (-> (json/write-str prs-request)
        (call-prs endpoint {:accept       "application/json"
                            :content-type "application/json"})
        (json/read-str :key-fn keyword))))

(defn build-prs-input
  [input]
  (-> (select-keys input [:user-connectors :placement-params])
      (assoc :components (-> input :module :components))))

(defn place-and-rank
  "Given the input map, calls PRS service and retuns the JSON returned by PRS.
   Input map
   {:module {:components [ {:module uri :vm-size '' }, ] }
    :placement-params { components: [ {:comp-uri uri :multiplicity # :policy string }, ] } ; map
    :prs-endpoint url ; str
    :user-connectors
      [{:user-connector: c1, :vm-sizes {:comp1 'tiny' :comp2 'huge'}}
       {:user-connector: c2, :vm-sizes {:comp1 'big' :comp2 'small'}}]
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

  (log/info "place-and-rank, input = " input)

  (json/write-str
    (if (empty? input)
      {}
      (prs-place-and-rank (:prs-endpoint input) (build-prs-input input)))))
