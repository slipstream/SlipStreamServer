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
   :policies        [{:protocol   "TCP"
                      :direction  "inbound"
                      :address    {:CIDR "192.168.0.0/24"}
                      :port       {:tcp-range [20 22]}}

                     {:protocol   "TCP"
                      :direction  "outbound"
                      :address    {:security-group-name "AnotherSecGroup"}
                      :port       {:tcp-range [30 56]}}]
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

(def valid-types ["Load Balancer" "QoS"])
;; TODO other types to implement
;; "Firewall" "VPN" "DHCP" "DNS" "NAT"
;; "Gateway" "Layer4 Port Forwarding" "IP Routing"
;; "Virtual Network Device" "Other"])

(defn firewall-with-state
  [state]
  (-> valid-firewall
      (assoc :state state)))

(deftest schema-state-values
  (tu/is-invalid?   (firewall-with-state "OF THE ART") NetworkService)
  (tu/are-valid?    (map firewall-with-state valid-states) NetworkService))

(deftest schema-type-values
  (tu/is-invalid?   (assoc valid-firewall :type "blah") NetworkService)
  (tu/is-valid?     valid-firewall              NetworkService)
  (tu/is-valid?     valid-load-balancer         NetworkService)
  (tu/is-valid?     valid-QoS                   NetworkService))

