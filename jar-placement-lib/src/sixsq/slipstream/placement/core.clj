(ns sixsq.slipstream.placement.core
  "
  Entry point is place-and-rank.
  Responsibility is to price given components on all given connectors.
  Each component explicits its preferred instance type per connector.
  Strategy to price one component
    * find the list of 'eligible' service offers. (a service offer that fulfill component contrainsts,
      or a service offer chosen by instance type).
    * for each connector the component is priced on the smallest service offer
  "
  (:require
    [clojure.tools.logging :as log]
    [clojure.string :as str]
    [sixsq.slipstream.placement.cimi-util :as cu]
    [sixsq.slipstream.pricing.lib.pricing :as pr]
    [sixsq.slipstream.client.api.cimi :as cimi]))

(def no-price -1)

(defn string-or-nil?
  [s]
  (or (nil? s) (string? s)))

(defn equals-ignore-case?
  [s1 s2]
  {:pre [(every? string-or-nil? [s1 s2])]}
  (or (every? nil? [s1 s2])
      (and (not-any? nil? [s1 s2]) (.equalsIgnoreCase s1 s2))))

(defn- in-description
  [attribute service-offer]
  (get-in service-offer [:schema-org:descriptionVector attribute]))

(def cpu (partial in-description :schema-org:vcpu))
(def ram (partial in-description :schema-org:ram))
(def disk (partial in-description :schema-org:disk))

(defn- instance-type
  [service-offer]
  (:schema-org:name service-offer))

(defn- connector-href
  [service-offer]
  (get-in service-offer [:connector :href]))

(defn- display-service-offer
  [service-offer]
  (str (connector-href service-offer) "/" (instance-type service-offer)))

(defn smallest-service-offer
  [service-offers]
  (->> service-offers
       (sort-by (juxt cpu ram disk))
       first))

(defn- EUR-or-unpriced?
  [service-offer]
  (let [price-currency (:schema-org:priceCurrency service-offer)]
    (or (nil? price-currency) (= "EUR" price-currency))))

(defn- smallest-service-offer-EUR
  [service-offers connector-name]
  (->> service-offers
       (filter #(and (EUR-or-unpriced? %) (= (get-in % [:connector :href]) connector-name)))
       smallest-service-offer))

(defn- fetch-service-offers
  [cimi-filter]
  (:serviceOffers (cimi/search (cu/context) "serviceOffers" (when cimi-filter {:$filter cimi-filter}))))

(defn- fetch-service-offers-rescue-reauthenticate
  [cimi-filter]
  (if-let [response (fetch-service-offers cimi-filter)]
    response
    (do
      (cu/update-context)
      (fetch-service-offers cimi-filter))))

(defn- denamespace
  [kw]
  (let [tokens (str/split (name kw) #":")
        cnt (count tokens)]
    (cond
      (= cnt 2) (keyword (second tokens))
      (= cnt 1) (keyword (first tokens))
      :else (keyword (apply str (rest tokens))))))

(defn denamespace-keys
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

;; TODO hard-coded currency to EUR and timecode to "HUR"
(defn- price-connector
  [service-offers connector-name]
  (if-let [service-offer (smallest-service-offer-EUR service-offers connector-name)]
    (let [price (compute-price service-offer "HUR")]
      (log/info "Priced " (display-service-offer service-offer) ":" price "EUR/h")
      {:name          connector-name
       :price         price
       :currency      "EUR"
       :cpu           (cpu service-offer)
       :ram           (ram service-offer)
       :disk          (disk service-offer)
       :instance_type (instance-type service-offer)})))

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

(defn- space
  [s]
  (str " " s " "))

(defn- paren
  [s]
  (str "(" s ")"))

(defn- cimi-op
  [op clauses]
  (->> clauses
       (remove empty?)
       (map paren)
       (str/join (space op))))

(def cimi-and (partial cimi-op "and"))

(def cimi-or (partial cimi-op "or"))

(defn- connector-same-instance-type
  [[connector-name instance-type]]
  (when instance-type
    (format "connector/href='%s' and schema-org:name='%s'" (name connector-name) instance-type)))

(defn- clause-connectors-same-instance-type
  [component]
  (mapv connector-same-instance-type (:connector-instance-types component)))

(defn- clause-cpu-ram-disk
  [component]
  (when (every? #(not (empty? (% component))) [:cpu.nb :ram.GB :disk.GB])
    (format
      "(schema-org:descriptionVector/schema-org:vcpu>=%sandschema-org:descriptionVector/schema-org:ram>=%sandschema-org:descriptionVector/schema-org:disk>=%s)"
      (:cpu.nb component)
      (:ram.GB component)
      (:disk.GB component))))

(def clause-flexible "schema-org:flexible='true'")

(defn- clause-component
  [component]
  (cimi-or (concat (clause-connectors-same-instance-type component)
                   [clause-flexible (clause-cpu-ram-disk component)])))

(defn- clause-connectors
  [connector-names]
  (->> connector-names
       (remove empty?)
       (mapv #(str "connector/href='" % "'"))
       cimi-or))

(defn- keep-only-exact-instance-type
  [connector-instance-types [connector-name service-offers]]
  (if-let [instance-type (get connector-instance-types (keyword connector-name))]
    (filter #(= instance-type (:schema-org:name %)) service-offers)
    service-offers))

(defn- service-offers-compatible-with-component
  [component connector-names]
  (let [cimi-filter (cimi-and [(clause-connectors connector-names)
                               (:placement-policy component)
                               (clause-component component)])
        _ (log/info (str "cimi-filter = '" cimi-filter "'"))
        service-offers (fetch-service-offers-rescue-reauthenticate cimi-filter)
        service-offers-by-connector-name (group-by connector-href service-offers)]

    (apply concat (map (partial keep-only-exact-instance-type (:connector-instance-types component))
                       service-offers-by-connector-name))))

(defn- place-rank-component
  [user-connectors component]
  (log/info "placing and ranking component " component)
  (log/info "user-connectors = " user-connectors)
  (let [filtered-service-offers (service-offers-compatible-with-component component user-connectors)]
    (log/info "filtered-service-offers = " (map display-service-offer filtered-service-offers))
    (price-component user-connectors filtered-service-offers component)))

(defn place-and-rank
  [request]
  (let [components (:components request)
        user-connectors (:user-connectors request)]
    {:components (map (partial place-rank-component user-connectors) components)}))
