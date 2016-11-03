(ns sixsq.slipstream.placement.core
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [sixsq.slipstream.placement.cimi-util :as cu]
    [sixsq.slipstream.pricing.lib.pricing :as pr]
    [sixsq.slipstream.client.api.cimi :as cimi]))

(def no-price -1)

(def service-offer-name-key :schema-org:name)
(def service-offer-currency-key :schema-org:priceCurrency)

(defn string-or-nil?
  [s]
  (or (nil? s) (string? s)))

(defn equals-ignore-case?
  [s1 s2]
  {:pre [(every? string-or-nil? [s1 s2])]}
  (or (every? nil? [s1 s2])
      (and (not-any? nil? [s1 s2]) (.equalsIgnoreCase s1 s2))))

(defn- entity-to-price-with
  [filtered-service-offers connector-name vm-size]
  (log/info "entity-to-price-with connector name " connector-name ", vm-size" vm-size)
  (let [entities (filter #(and
                           (equals-ignore-case? (service-offer-name-key %) vm-size)
                           (= (service-offer-currency-key %) "EUR")
                           (= (get-in % [:connector :href]) connector-name))
                         filtered-service-offers)
        nb-entities (count entities)]
    (if (= 1 nb-entities)
      (do
        (log/info "For " connector-name ", " vm-size ": OK")
        (first entities))
      (do
        (log/warn "For " connector-name ", " vm-size ", expected one entity, found " nb-entities)
        nil))))

(defn- fetch-service-offers
  [cimi-filter]
  (:serviceOffers (cimi/search (cu/context) "serviceOffers" (when cimi-filter {:$filter cimi-filter}))))

(defn- denamespace
  [kw]
  (let [tokens (str/split (name kw) #":")
        cnt (count tokens)]
    (cond
      (= cnt 2) (keyword (second tokens))
      (= cnt 1) (keyword (first tokens))
      :else (keyword (apply str (rest tokens))))))

(defn- denamespace-keys
  [m]
  (if (map? m)
    (into {} (map (fn [[k v]] [(denamespace k) (denamespace-keys v)]) m))
    m))

(defn- compute-price
  [filtered-service-offers component connector timecode]
  (log/info "compute price component " component)
  (log/info "compute price connector " connector)
  (let [vm-size (get-in connector [:vm-sizes (-> component keyword)])
        connector-name (:user-connector connector)
        entity (entity-to-price-with filtered-service-offers connector-name vm-size)
        entity (denamespace-keys entity)]
    (if entity
      (do
        (log/info "Pricing with entity: " entity)
        (pr/compute-cost entity
                         [{:timeCode timecode
                           :sample   1
                           :values   [1]}]))
      no-price)))

(defn- price-connector
  [filtered-service-offers component user-connector]
  {:name     (:user-connector user-connector)
   :price    (compute-price filtered-service-offers component user-connector "HUR") ;; TODO hard-coded timecode
   :currency "EUR"})                                        ;; TODO hard-coded currency to EUR

(defn order-by-price
  "Orders by price ascending, with the exception of no-price values placed at the end"
  [priced-coll]
  (sort-by :price (fn [a b]
                    (cond (= a -1) 1
                          (= b -1) -1
                          :else (< a b))) priced-coll))
(defn- add-indexes
  [coll]
  (map-indexed (fn [i e] (assoc e :index i)) coll))

(defn price-component
  [user-connectors filtered-service-offers component]
  (log/info "will price component" component " for user connectors " user-connectors " with offers"
            filtered-service-offers)
  {:node       (:node component)
   :module     (:module component)
   :connectors (->> (map (partial price-connector filtered-service-offers (:module component)) user-connectors)
                    order-by-price
                    add-indexes)})

(defn cimi-and
  [clause1 clause2]
  (log/info (str "clause1 '" clause1 "', clause2 '" clause2 "'"))
  (cond
    (every? empty? [clause1 clause2]) ""
    (empty? clause1) clause2
    (empty? clause2) clause1
    :else (str "(" clause1 ") and (" clause2 ")")))

(defn- clause-cpu-ram-disk
  [component]
  (format "schema-org:descriptionVector/schema-org:vcpu>=%sandschema-org:descriptionVector/schema-org:ram>=%sandschema-org:descriptionVector/schema-org:disk>=%s"
          (:cpu.nb component)
          (:ram.GB component)
          (:disk.GB component)
          "bob"))

(defn cimi-filter-policy
  [connectors component]
  (let [policy (:placement-policy component)
        connector-names (map :user-connector connectors)
        connectors-clauses (mapv #(str "connector/href='" % "'") connector-names)
        connectors-clause (str/join " or " connectors-clauses)
        cimi-filter (cimi-and policy connectors-clause)
        cimi-filter (cimi-and cimi-filter (clause-cpu-ram-disk component))]
    (log/info "the cimi-filter = " cimi-filter)
    cimi-filter))

(defn- service-offers-by-component-policy
  [user-connectors component]
  (log/info "Fetching service offers by component policy")
  (fetch-service-offers (cimi-filter-policy user-connectors component)))

(defn filter-user-connectors
  [user-connectors filtered-service-offers component]
  (if (empty? (:placement-policy component))
    user-connectors
    (let [set-user-connector-names (->> filtered-service-offers (map #(-> % :connector :href)) set)]
      (filter #(set-user-connector-names (:user-connector %)) user-connectors))))

(defn- place-rank-component
  [user-connectors component]
  (let [filtered-service-offers (service-offers-by-component-policy user-connectors component)
        filtered-user-connectors (filter-user-connectors user-connectors filtered-service-offers component)]
    (price-component filtered-user-connectors filtered-service-offers component)))

(defn place-and-rank
  [request]
  (log/info "place-and-rank, request = " request)
  (let [components (:components request)
        user-connectors (:user-connectors request)]
    {:components (map (partial place-rank-component user-connectors) components)}))
