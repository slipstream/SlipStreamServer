(ns com.sixsq.slipstream.ssclj.resources.network-service-test
  (:require
    [clojure.test                                         :refer :all]
    [com.sixsq.slipstream.ssclj.resources.network-service :refer :all]
    [com.sixsq.slipstream.ssclj.resources.test-utils      :as tu]))


(def acl {:owner {:type "USER"   :principal "john"}
          :rules [{:type "USER"  :principal "ANON" :right "VIEW"}]})

(def incomplete-network-service
  {
   :acl             acl
   :id              "NetworkService/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :resourceURI     resource-uri

   :state           "STARTED"
   :type            "Firewall"
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
  (-> incomplete-network-service
      (assoc :policies [{:protocol "TCP"}])
      (assoc :state state)))

(defn firewall-with-type
  [type]
  (-> incomplete-network-service
      (assoc :policies {:protocol "TCP"})
      (assoc :type type)))

(deftest schema-state-values
  (tu/is-invalid?   (firewall-with-state "OF THE ART") NetworkService)
  (tu/are-valid?    (map firewall-with-state valid-states) NetworkService))
;
(deftest schema-type-values
  (tu/is-invalid?   (firewall-with-type "blah") NetworkService)
  (tu/are-valid?    (map firewall-with-type valid-types) NetworkService))
