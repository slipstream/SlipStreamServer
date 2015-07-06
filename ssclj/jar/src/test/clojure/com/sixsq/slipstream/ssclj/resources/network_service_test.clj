(ns com.sixsq.slipstream.ssclj.resources.network-service-test
  (:require
    [clojure.test                                         :refer :all]
    [com.sixsq.slipstream.ssclj.resources.network-service :refer :all]
    [com.sixsq.slipstream.ssclj.resources.test-utils      :as tu]))


(def acl {:owner {:type "USER"   :principal "john"}
          :rules [{:type "USER"  :principal "ANON" :right "VIEW"}]})

(def valid-firewall
  {
   :acl             acl
   :id              "NetworkService/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :resourceURI     resource-uri

   :state           "STARTED"
   :type            "Firewall"
   :policies        {:rules
                      [ { :protocol   "TCP"
                          :direction  "inbound"
                          :address    {:CIDR "192.168.0.0/24"}
                          :port       {:tcp-range [1 22]}}

                        { :protocol   "TCP"
                          :direction  "outbound"
                          :address    {:security-group-name "AnotherSecGroup"}
                          :port       {:tcp-range [30 56]}}]}
   })

(def valid-load-balancer
  {
   :acl             acl
   :id              "NetworkService/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :resourceURI     resource-uri

   :state           "STARTED"
   :type            "Load Balancer"
   :policies        {}
   })

(def valid-QoS
  {
   :acl             acl
   :id              "NetworkService/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :resourceURI     resource-uri

   :state           "STARTED"
   :type            "QoS"
   :policies        {}
   })

(def valid-states ["CREATING" "STARTED" "STOPPED" "ERROR"])

;; TODO other types to implement
(def valid-types ["Load Balancer" "QoS"])

(defn firewall-with-state
  [state]
  (-> valid-firewall
      (assoc :state state)))

(defn firewall-with-rule
  [rule]
  (-> valid-firewall
      (assoc-in [:policies :rules] [rule])))

(deftest schema-state-values
  (tu/is-invalid?   (firewall-with-state "OF THE ART") NetworkService)
  (tu/are-valid?    (map firewall-with-state valid-states) NetworkService))

(deftest schema-tcp-port
  (doseq [invalid-port-range [ [] [1] [1 2 3] ["1" 2] [1 "2"]]]
    (let [invalid-rule  { :protocol   "TCP"
                          :direction  "inbound"
                          :address    {:CIDR "192.168.0.0/24"}
                          :port       {:tcp-range invalid-port-range}}]
      (tu/is-invalid? (firewall-with-rule invalid-rule) NetworkService))))

(deftest schema-icmp
  (let [rule  { :protocol   "TCP"
                :direction  "inbound"
                :address    {:CIDR "192.168.0.0/24"}
                :port       {:icmp {:type 8 :code 0}}}]
    (tu/is-valid? (firewall-with-rule rule) NetworkService)))

(deftest schema-type-values
  (tu/is-invalid?   (assoc valid-firewall :type "blah") NetworkService)
  (tu/is-valid?     valid-firewall              NetworkService)
  (tu/is-valid?     valid-load-balancer         NetworkService)
  (tu/is-valid?     valid-QoS                   NetworkService))

