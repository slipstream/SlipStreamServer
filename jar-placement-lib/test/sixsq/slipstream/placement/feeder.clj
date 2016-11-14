(ns sixsq.slipstream.placement.feeder
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [clojure.walk :as walk]
    [sixsq.slipstream.placement.cimi-util :as cu]
    [sixsq.slipstream.client.api.cimi :as cimi]))

(defn- find-resource
  [resource-path]
  (if-let [config-file (io/resource resource-path)]
    (do
      (log/info "Will use " (.getPath config-file) " as config file")
      config-file)
    (let [msg (str "Resource not found (must be in classpath): '" resource-path "'")]
      (log/error msg)
      (throw (IllegalArgumentException. msg)))))

(defn- store-service-offer!
  [service-offer]
  (log/info "storing" service-offer)
  (println (cimi/add (cu/context) "serviceOffers" service-offer)))

(defn feed-service-offer
  "Reads JSON files and store data in service-offer resource"
  []
  (let [json-path "data/exoscale_price_data.json"]
    (println "reading " json-path)
    (->> json-path
         find-resource
         slurp
         json/read-str
         walk/keywordize-keys
         :instances
         (run! store-service-offer!))))
