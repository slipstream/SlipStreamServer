(ns sixsq.slipstream.placement.core
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [sixsq.slipstream.placement.cimi-util :as cu]
    [sixsq.slipstream.pricing.lib.pricing :as pr]
    [sixsq.slipstream.client.api.cimi :as cimi]))

(def service-offer-currency-key :schema-org:priceCurrency)
(def no-price -1)

(defn string-or-nil?
  [s]
  (or (nil? s) (string? s)))

(defn equals-ignore-case?
  [s1 s2]
  {:pre [(every? string-or-nil? [s1 s2])]}
  (or (every? nil? [s1 s2])
      (and (not-any? nil? [s1 s2]) (.equalsIgnoreCase s1 s2))))

(defn- display-service-offer
  [service-offer]
  (str "ServiceOffer " (get-in service-offer [:connector :href]) "/" (:schema-org:name service-offer)))

(defn smallest-service-offer
  [service-offers]
  (->> service-offers
       (sort-by (juxt #(get-in % [:schema-org:descriptionVector :schema-org:vcpu])
                      #(get-in % [:schema-org:descriptionVector :schema-org:ram])
                      #(get-in % [:schema-org:descriptionVector :schema-org:disk])))
       first))

(defn- EUR-or-unpriced?
  [service-offer]
  (let [price-currency (service-offer-currency-key service-offer)]
    (or (nil? price-currency) (= "EUR" price-currency))))

(defn- service-offer-for-connector
  [service-offers connector-name]
  (->> service-offers
       (filter #(and (EUR-or-unpriced? %)
                     (= (get-in % [:connector :href]) connector-name)))
       smallest-service-offer))

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

(defn- priceable?
  [service-offer]
  (every? (set (keys service-offer))
          [:schema-org:billingTimeCode
           :schema-org:price
           :schema-org:priceCurrency
           :schema-org:unitCode]))

(defn- compute-price
  [service-offer timecode]
  (if (priceable? service-offer)
    (pr/compute-cost (denamespace-keys service-offer)
                     [{:timeCode timecode
                       :sample   1
                       :values   [1]}])
    no-price))

(defn- price-connector
  [service-offers connector-name]
  (if-let [service-offer (service-offer-for-connector service-offers connector-name)]
    (do
      (log/info "Entity to price with " (display-service-offer service-offer))
      {:name     connector-name
       :price    (compute-price service-offer "HUR")               ;; TODO hard-coded timecode
       :currency "EUR"})))                                  ;; TODO hard-coded currency to EUR

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
  [user-connectors service-offers component]
  {:node       (:node component)
   :module     (:module component)
   :connectors (->> (map (partial price-connector service-offers) user-connectors)
                    (remove nil?)
                    order-by-price
                    add-indexes)})

(defn cimi-and
  [clause1 clause2]
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
          (:disk.GB component)))

(defn cimi-filter-policy
  [connector-names component]
  (let [policy              (:placement-policy component)
        connectors-clauses  (mapv #(str "connector/href='" % "'") connector-names)
        connectors-clause   (str/join " or " connectors-clauses)
        cimi-filter         (cimi-and policy connectors-clause)
        cimi-filter         (cimi-and cimi-filter (clause-cpu-ram-disk component))]
    (log/debug "the cimi-filter = " cimi-filter)
    cimi-filter))

(defn- service-offers-by-component-policy
  [user-connectors component]
  (fetch-service-offers (cimi-filter-policy user-connectors component)))

(defn- place-rank-component
  [user-connectors component]
  (log/info "component is: " component)
  (let [filtered-service-offers (service-offers-by-component-policy user-connectors component)]
    (log/info "filtered-service-offers = " (map :id filtered-service-offers))
    (log/info "user-connectors = " user-connectors)
    (price-component user-connectors filtered-service-offers component)))

(defn place-and-rank
  [request]
  (log/info "Calling place-and-rank")
  (let [components (:components request)
        user-connectors (:user-connectors request)]
    {:components (map (partial place-rank-component user-connectors) components)}))
