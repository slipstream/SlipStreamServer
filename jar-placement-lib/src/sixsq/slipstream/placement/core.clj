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

(defn- denamespace
       [kw]
       (let [tokens (str/split (name kw) #":")
             cnt (count tokens)]
            (cond
              (= cnt 2) (keyword (second tokens))
              (= cnt 1) (keyword (first tokens))
              :else (keyword (str/join (rest tokens))))))

(defn denamespace-keys
      [m]
      (if (map? m)
        (into {} (map (fn [[k v]] [(denamespace k) (denamespace-keys v)]) m))
        m))

(defn cpu [service-offer] (-> service-offer :resource:vcpu))
(defn ram [service-offer] (-> service-offer :resource:ram))
(defn disk [service-offer] (-> service-offer :resource:disk))

(defn parse-number [s]
  (let [digit (re-find #"[\d.]+" (str s))]
    (when (not-empty digit)
      (read-string digit))))

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
  (let [price-currency (:price:currency service-offer)]
    (or (nil? price-currency) (= "EUR" price-currency))))

(defn- smallest-service-offer-EUR
  [service-offers connector-name]
  (->> service-offers
       (filter #(and (EUR-or-unpriced? %) (= (get-in % [:connector :href]) connector-name)))
       smallest-service-offer))

(defn- fetch-service-offers
  [cimi-filter]
  (let [result (cimi/search (cu/context) "serviceOffers" (when cimi-filter {:$filter cimi-filter}))]
    (if (instance? Exception result)
      (do
        (log/error "exception when querying service offers; status =" (:status (ex-data result)))
        (throw result))
      (do
        (log/debug "selected service offers:" result)
        (:serviceOffers result)))))

(defn- fetch-service-offers-rescue-reauthenticate
  [cimi-filter]
  (try
    (fetch-service-offers cimi-filter)
    (catch Exception ex
      (if (#{401 403} (:status (ex-data ex)))
        (try
          (log/error "retrying query")
          (cu/update-context)
          (let [result (fetch-service-offers cimi-filter)]
            (log/error "retry succeeded")
            result)
          (catch Exception ex
            (log/error "retry failed; sending nil result")
            nil))))))

(defn- priceable?
  [service-offer]
  (every? (set (keys service-offer))
          [:price:billingUnitCode
           :price:unitCost
           :price:currency
           :price:unitCode]))

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
      (log/debug "Priced " (display-service-offer service-offer) ":" price "EUR/h")
      {:name          connector-name
       :price         price
       :currency      "EUR"
       :cpu           (cpu service-offer)
       :ram           (ram service-offer)
       :disk          (disk service-offer)
       :instance_type (instance-type service-offer)
       :service-offer service-offer})))

(defn number-or-nil
  "If the value is a non-negative number, then the number is returned.  Otherwise
   nil is returned.  This is a utility function for the price-comparator function."
  [a]
  (when (and (number? a) (not (neg? a))) a))

(defn price-comparator
  "Sorts prices from lowest to highest prices. Any value that isn't a non-negative
   number (sentinel values for 'no price') will compare higer than any value.
   This implements a two-value comparator as described in the clojure
   documentation: https://clojure.org/guides/comparators."
  [a b]
  (let [a (number-or-nil a)
        b (number-or-nil b)]
    (cond
      (= a b) false
      (nil? a) false
      (nil? b) true
      :else (< a b))))

(defn order-by-price
  "Orders by price ascending, with the exception of no-price values placed at the end"
  [priced-coll]
  (sort-by :price price-comparator priced-coll))

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

(defn to-MB-from-GB [input]
  (when input (* input 1024)))

(defn clause-cpu-ram-disk-os
  [{cpu :cpu.nb, ram :ram.GB, disk :disk.GB, os :operating-system}]
  (format
    "(resource:vcpu>=%s and resource:ram>=%s and resource:disk>=%s and resource:operatingSystem='%s')"
    (or cpu 0) (or (to-MB-from-GB (parse-number ram)) 0) (or disk 0) os))

(def clause-flexible "schema-org:flexible='true'")

(defn- clause-component
  [component]
  (cimi-or (concat [clause-flexible (clause-cpu-ram-disk-os component)])))

(defn- clause-connectors
  [connector-names]
  (->> connector-names
       (remove empty?)
       (mapv #(str "connector/href='" % "'"))
       cimi-or))

(defn extract-favorite-offers [service-offers {instance-type :instance.type cpu :cpu ram :ram disk :disk}]
  (cond->>
    service-offers
    instance-type (filter #(= instance-type (:schema-org:name %)))
    disk (filter #(= (parse-number disk) (:resource:disk %)))
    (and cpu (not instance-type)) (filter #(= (parse-number cpu) (:resource:vcpu %)))
    (and ram (not instance-type)) (filter #(= (to-MB-from-GB (parse-number ram)) (:resource:ram %)))))

(defn prefer-exact-instance-type
  [connector-instance-types [connector-name service-offers]]
  (if-let [connector-params (get connector-instance-types(keyword connector-name))]
    (let [favorite (extract-favorite-offers service-offers connector-params)]
      (if (empty? favorite)
             service-offers
             favorite))
    service-offers))

(defn- service-offers-compatible-with-component
  [component connector-names]
  (let [cimi-filter (cimi-and [(clause-connectors connector-names)
                               (:placement-policy component)
                               (clause-component component)])
        _ (log/debug (str "cimi filter: '" cimi-filter "'"))
        service-offers (fetch-service-offers-rescue-reauthenticate cimi-filter)
        _ (log/debug (str "number of matching service offers:" (count service-offers)))
        service-offers-by-connector-name (group-by connector-href service-offers)
        _ (log/debug (str "number of matching clouds:" (count service-offers-by-connector-name)))]

    (mapcat (partial prefer-exact-instance-type (:connector-instance-types component))
            service-offers-by-connector-name)))

(defn- place-rank-component
  [connectors component]
  (log/debug "component:" component)
  (log/debug "connectors:" connectors)
  (let [filtered-service-offers (service-offers-compatible-with-component component connectors)]
    (log/info "filtered offers" (map display-service-offer filtered-service-offers) "for component" (:module component))
    (price-component connectors filtered-service-offers component)))

(defn place-and-rank
  [request]
  (let [components (:components request)
        user-connectors (:user-connectors request)
        result (map (partial place-rank-component user-connectors) components)]
    {:components result}))
