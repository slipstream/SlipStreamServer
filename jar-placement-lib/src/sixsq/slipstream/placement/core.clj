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
    [clojure.core.async :as async]
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

(defn map-async-multithreaded
  [number-thread blocking-function input-coll]
  (let [output-chan (async/chan)]
    (async/pipeline-blocking number-thread
                             output-chan
                             (map blocking-function)
                             (async/to-chan input-coll))
    (async/<!! (async/into [] output-chan))
    ))

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

(defn- service-offer-name
  [service-offer]
  (:name service-offer))

(defn- connector-href
  [service-offer]
  (get-in service-offer [:connector :href]))

(defn- display-service-offer
  [service-offer]
  (str (connector-href service-offer) "/" (service-offer-name service-offer)))

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
  [cimi-filter cimi-orderby cimi-first cimi-last]
  (let [request-parameter (cond-> {}
                                  cimi-filter  (assoc :$filter cimi-filter)
                                  cimi-orderby (assoc :$orderby cimi-orderby)
                                  cimi-first   (assoc :$first cimi-first)
                                  cimi-last    (assoc :$last cimi-last))
        result (cimi/search (cu/context) "serviceOffers" request-parameter)]
    (if (instance? Exception result)
      (do
        (log/error "exception when querying service offers; status =" (:status (ex-data result)))
        (throw result))
      (do
        (log/debug "selected service offers:" result)
        (:serviceOffers result)))))

(defn- fetch-service-offers-rescue-reauthenticate
  [cimi-filter cimi-orderby cimi-first cimi-last]
  (try
    (fetch-service-offers cimi-filter cimi-orderby cimi-first cimi-last)
    (catch Exception ex
      (if (#{401 403} (:status (ex-data ex)))
        (try
          (log/error "retrying query")
          (cu/update-context)
          (let [result (fetch-service-offers cimi-filter cimi-orderby cimi-first cimi-last)]
            (log/error "retry succeeded")
            result)
          (catch Exception ex
            (log/error "retry failed; sending nil result")
            nil))
        (log/error "got an HTTP error while querying service offers: " (ex-data ex))))))

(defn- priceable?
       [service-offer]
       (every? #(get service-offer %)
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
      {:connector connector-name
       :price     price
       :currency  "EUR"
       :name      (service-offer-name service-offer)
       :id        (:id service-offer)})))

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
  (let [a-price (number-or-nil (first a))
        b-price (number-or-nil (first b))
        a-connector-name (second a)
        b-connector-name (second b)]
    (cond
      (= a-price b-price) (= (first (sort [a-connector-name b-connector-name])) a-connector-name)
      (nil? a-price) false
      (nil? b-price) true
      :else (< a-price b-price))))

(defn order-by-price
  "Orders by price ascending, with the exception of no-price values placed at the end"
  [priced-coll]
  (sort-by (juxt :price :connector) price-comparator priced-coll))

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
  (when input (int (* input 1024))))

(defn clause-cpu-ram-disk
  [{cpu :cpu.nb, ram :ram.GB, disk :disk.GB}]
  (let [cpu (or (parse-number cpu) 0)
        ram (or (to-MB-from-GB (parse-number ram)) 0)
        disk (or (parse-number disk) 0)]
    (cimi-and [(str "resource:vcpu>=" cpu)
               (str "resource:ram>="  ram)
               (str "resource:disk>=" disk)])))

(defn- clause-connector-specific
  [specific-connector-options]
  (let [instance-type (:instance.type specific-connector-options)
        cpu (:cpu specific-connector-options)
        ram (:ram specific-connector-options)
        disk (:disk specific-connector-options)]
    (cimi-and
      (cond-> []
              instance-type (conj (format "resource:instanceType='%s'" instance-type))
              cpu (conj (format "resource:vcpu>=%s" (parse-number cpu)))
              ram (conj (format "resource:ram>=%s" (to-MB-from-GB (parse-number ram))))
              disk (conj (format "resource:disk>=%s" (parse-number disk)))))))

(defn- fetch-service-offer-compatible-connector
  [clause-placement
   {cpu :cpu.nb, ram :ram.GB, disk :disk.GB, os :operating-system,
    connector-instance-types :connector-instance-types :as component}
   connector]
  (let [clause-vm         "resource:type='VM'"
        clause-flexible   "schema-org:flexible='true'"
        clause-os         (format "resource:operatingSystem='%s'" os)
        clause-size       (if (or cpu ram disk)
                            (clause-cpu-ram-disk component)
                            (clause-connector-specific ((keyword connector) connector-instance-types)))
        clause-connector  (format "connector/href='%s'" connector)
        clause-request    (->> (cimi-and [clause-vm clause-os clause-size])
                               (conj [clause-flexible])
                               cimi-or
                               (conj [clause-connector clause-placement])
                               cimi-and)
        orderby           "price:unitCost:asc,resource:vcpu:asc,resource:ram:asc,resource:disk:asc"
        last              1]
    (log/debug "Module name: " (:module component) " Clause request: " clause-request)
    (fetch-service-offers-rescue-reauthenticate clause-request orderby nil first)))

(defn- compute-service-offers
  [number-threads clause-placement connectors component]
  (let [result (map-async-multithreaded number-threads
                                        (partial fetch-service-offer-compatible-connector clause-placement component)
                                        connectors)]
    (->> result (apply concat))))

(defn- place-rank-component
  [number-threads placement-params connectors component]
  (log/debug "component:" component)
  (log/debug "connectors:" connectors)
  (let [module (:module component)
        clause-placement ((keyword module) placement-params)
        filtered-service-offers (compute-service-offers number-threads clause-placement connectors component)]
    (log/debug "filtered offers"
               (map display-service-offer filtered-service-offers) "for component" (:module component))
    (price-component connectors filtered-service-offers component)))

(defn place-and-rank
  [request]
  (cu/context)
  (let [components       (:components request)
        connectors       (:user-connectors request)
        placement-params (:placement-params request)
        max-thread                 30
        number-threads-connectors (max 1 (min max-thread (count connectors)))
        number-threads-components (max 1 (min (int (/ max-thread number-threads-connectors)) (count components)))
        result (map-async-multithreaded number-threads-components
                                        (partial place-rank-component number-threads-connectors
                                                 placement-params connectors)
                                        components)]
    (log/info "Number of threads: "
              (str number-threads-components " * " number-threads-connectors " = "
                   (* number-threads-connectors number-threads-components)))
    {:components result}))
