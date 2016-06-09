(ns sixsq.slipstream.prs.core
  "
  # Library for interaction with Placement and Ranking Service.
  "
  {:doc/format :markdown}
  (:require
    [clojure.data.json :as json]))

(defn prs-place-and-rank
  "
  Input request map:
  {:module uri
   :components {:module uri
                }
   :connectors [c1 c2]
  }

  Output
  {
   :components [
                { module: <comp1-uri>, connectors: [{ name: cloud1, price: 1, currency: USD },
                                                    { name: cloud2, price: 2, currency: USD }] },
                { module: <comp2-uri>, connectors: [{ name: cloud1, price: 1, currency: USD }] },
                ]
   }
  "
  [endpoint request]
  {:components [{:module "module/foo", :connectors [{:name "c1", :price 1, :currency "USD"},
                                                    {:name "c2", :price 2, :currency "USD"}]}
                {:module "module/bar", :connectors [{:name "c1", :price 1, :currency "USD"}]}]
   })

(defn place-and-rank
  "Given the input map, calls PRS service and retuns the JSON returned by PRS.
   Input map
   {
     :module {:module uri
              :components [ {:comp-uri uri :multiplicity # }, ] }
     :placement-params { components: [ {:comp-uri uri :multiplicity # :policy string }, ] } ; map
     :prs-endpoint url ; str
     :user-connectors [\"c1\" \"c2\"] ; vector
     }


  Output app
  {
   module: <app-uri>,
   components: [
                { module: <comp1-uri>, connectors: [{ name: cloud1, price: 123.123, currency: USD },
                                                    { name: cloud2, price: 345.123, currency: USD }] },
                { module: <comp2-uri>, connectors: [{ name: cloud1, price: 123.123, currency: USD }] },
                ]
   }
  Output component
  {
   module: <comp-uri>,
   components: [
                { module: <comp-uri>, connectors: [{ name: cloud1, price: 123.123, currency: USD },
                                                   { name: cloud2, price: 345.123, currency: USD }] }
                ]
   }
   "
  [input]
  (if (empty? input)
    (json/write-str {})
    (prs-place-and-rank (:prs-endpoint input) input)
    )
  )


