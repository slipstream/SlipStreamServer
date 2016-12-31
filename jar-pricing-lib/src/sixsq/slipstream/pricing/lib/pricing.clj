(ns sixsq.slipstream.pricing.lib.pricing)

;;UTIL FUNCTIONS
(defn- >0? [v] (> v 0))
(defn- no-unit? [v] (= v "C62"))
(defn- is-defined? [v] (not (no-unit? v)))
(defn- +list [l] (reduce + l))

;;TIME CONVERSION & HANDLING FUNCTIONS
(def timecode-hours {:ANN 1/8760
                     :MON 1/730
                     :WEE 1/168
                     :DAY 1/24
                     :HUR 1
                     :MIN 60
                     :SEC 3600})

(defn- convert-time
  ([t tc1 tc2 rounded?]
   (let [v1 ((keyword tc1) timecode-hours)
         v2 ((keyword tc2) timecode-hours)
         value (* (/ t v1) v2)]
     (if rounded? (Math/ceil value) value)))
  ([t tc1 tc2]
   (convert-time t tc1 tc2 false)))

(defn- timesampling>?
  [t tc1 tc2]
  (> (convert-time t tc1 "HUR") (convert-time 1 tc2 "HUR")))

(defn sample-periods
  [entity quantity]
  (let [t (convert-time (:sample quantity) (:timeCode quantity) (:sampleTimeCode entity) false)]
    (Math/ceil (* t (count (:values quantity))))))

(defn aggregate-for-billing-period
  [entity samples]
  (let [n (convert-time 1 (:billingTimeCode entity) (:sampleTimeCode entity) false)
        coll (partition n n [] samples)]
    (map +list coll)))

(defn units-per-sampling-period
  [entity quantity]
  (if (not (timesampling>? (:sample quantity) (:timeCode quantity) (:sampleTimeCode entity)))
    ;then aggregate
    (do                                                     ;(println "aggregate" (sample-periods entity quantity) (count (:values quantity)))
      (let [step (convert-time (:sample quantity) (:timeCode quantity) (:sampleTimeCode entity) false)
            n (int (/ 1 step))
            coll (partition n n [] (:values quantity))]
        (map +list coll)))
    ;else repeat
    (do                                                     ;(println "repeat" (sample-periods entity quantity) (count (:values quantity)))
      (let [bp (sample-periods entity quantity)
            f (int (/ bp (count (:values quantity))))
            ;This may not be correct.
            ;Compare billing period to sampling period maybe ?
            coll (map #(cons % (repeat (- f 1) (if (no-unit? (:unitCode entity)) % 0))) (:values quantity))]
        (reduce concat coll)))))





;;DISCOUNT FUNCTIONS
(defn- elem-price
  [el {price :price}]
  (* (- 1 (:p el)) (:q el) price))

(defn- progressive-discount
  [{{:keys [steps]} :discount :as entity} q]
  (reduce
    #(+ %1 (elem-price %2 entity))
    0
    (do (defn discount
          [value remsteps result]
          (if (>0? value)
            (let [step (first remsteps)
                  remvalue (- value (:q step))
                  tail (rest remsteps)]
              (recur remvalue
                     (if (empty? tail) remsteps tail)
                     (conj result {:p (:disc step) :q (if (>0? remvalue) (:q step) value)})
                     )
              )
            result
            )
          )
        (discount q steps [])
        )
    )
  )


(def discount-functions {:None        (fn [entity q] (* (:price entity) q))
                         ;NB : Reservation is equal to no discount in a billing context
                         :Reservation (fn [entity q] (* (:price entity) q))
                         :Progressive progressive-discount}
  )


;;COST COMPUTING FUNCTIONS

(defn- get-cost
  [{{:keys [method]} :discount :as entity} q]
  (let [costfunc ((keyword method) discount-functions)]
    (costfunc entity q)
    )
  )


(defn compute-cost
  [entity quantity]
  (let [samples (units-per-sampling-period entity (first quantity))
        agg-samples (aggregate-for-billing-period entity samples)
        costs (map #(get-cost entity %) agg-samples)
        tot (+list costs)]
    (+list
      (cons tot
            (map #(compute-cost %1 [%2])
                 (:associatedCosts entity)
                 (rest quantity))
            )
      )
    )
  )

;;EXAMPLE/TEST VALUES. NOT UP TO DATE
(def gce-instance {
                   :description       "n1-standard-1"
                   :freeQuantity      0
                   :price             0.05
                   :discount          {
                                       :method "Progressive"
                                       :reset  true
                                       :steps  [{:q 182.5 :disc 0} {:q 182.5 :disc 0.2} {:q 182.5 :disc 0.4} {:q 182.5 :disc 0.6}]
                                       }
                   :currency          "USD"
                   :associatedCosts   []
                   :timeCode          "HUR"
                   :billingTimeCode   "MON"
                   :sampleTimeCode    "HUR"
                   :unitCode          "C62"
                   :descriptionVector {:vcpu 1.0 :ram 3.75 :disk 0.0}
                   }
  )

(def gce-disk {
               :description       "GCE SSD provisioned space"
               :freeQuantity      0
               :price             (/ 0.17 730)
               :discount          {
                                   :method "None"
                                   :reset  false
                                   :steps  []
                                   }
               :currency          "USD"
               :associatedCosts   []
               :timeCode          "HUR"
               :unitCode          "GBH"
               :descriptionVector {}
               }
  )

(def exo-instance {
                   :description       "Micro"
                   :freeQuantity      0
                   :price             (/ 0.0070 60)         ;0.0084 / heure w/ disk (-0.0014)
                   :discount          {
                                       :method "None"
                                       :reset  false
                                       :steps  []
                                       }
                   :currency          "USD"
                   :associatedCosts   []
                   :timeCode          "MIN"
                   :billingTimeCode   "HUR"
                   :sampleTimeCode    "MIN"
                   :unitCode          "C62"
                   :descriptionVector {:vcpu 1.0 :ram 0.512 :disk 10.0}
                   }
  )

(def exo-disk {
               :description       "Exoscale 10 GB SSD Disk"
               :freeQuantity      0
               :price             (/ 0.0014 60)
               :discount          {
                                   :method "None"
                                   :reset  false
                                   :steps  []
                                   }
               :currency          "USD"
               :associatedCosts   []
               :timeCode          "MIN"
               :unitCode          "C62"
               :descriptionVector {:disk 10.0}
               }
  )

(def gce-network
  {
   :description       "GCE outgoing network"
   :freeQuantity      0
   :price             0.12
   :discount          {
                       :method "Progressive"
                       :reset  false
                       :steps  [{:q 1024 :disc 0} {:q 9216 :disc 1/12} {:q 0xffff :disc 1/3}]
                       }
   :currency          "USD"
   :associatedCosts   []
   :billingTimeCode   "MON"
   :sampleTimeCode    "MON"
   :unitCode          "GBM"
   :descriptionVector {:vcpu 1.0 :ram 3.75 :disk 0.0}
   }
  )

(def gce-os
  {
   :description       "GCE object storage - storage price"
   :freeQuantity      0
   :price             0.026
   :discount          {
                       :method "None"
                       :reset  false
                       :steps  []
                       }
   :currency          "USD"
   :associatedCosts   [
                       {:description       "GCE object sotrage class A operations"
                        :freeQuantity      0
                        :price             1e-06
                        :discount          {
                                            :method "None"
                                            :reset  false
                                            :steps  []
                                            }
                        :currency          "USD"
                        :associatedCosts   []
                        :timeCode          "MON"
                        :billingTimeCode   "MON"
                        :sampleTimeCode    "MON"
                        :unitCode          "RMO"
                        :descriptionVector {}
                        }
                       {:description       "GCE object sotrage class B operations"
                        :freeQuantity      0
                        :price             1e-05
                        :discount          {
                                            :method "None"
                                            :reset  false
                                            :steps  []
                                            }
                        :currency          "USD"
                        :associatedCosts   []
                        :timeCode          "MON"
                        :unitCode          "RMO"
                        :billingTimeCode   "MON"
                        :sampleTimeCode    "MON"
                        :descriptionVector {}
                        }
                       ]
   :timeCode          "MON"
   :billingTimeCode   "MON"
   :sampleTimeCode    "MON"
   :unitCode          "GBM"
   :descriptionVector {}
   }
  )


(def qt {:instance [
                    {:sample   1
                     :timeCode "HUR"
                     :values   [1 1 1 1 1 1]
                     }]
         :network  [
                    {:sample   1
                     :timeCode "MON"
                     :values   [10000]
                     }]
         :os       [
                    {:sample   1
                     :timeCode "WEE"
                     :values   [1000 9000]
                     }
                    {:sample   1
                     :timeCode "WEE"
                     :values   [10000]
                     }
                    {:sample   1
                     :timeCode "WEE"
                     :values   [10000]
                     }
                    ]
         })
