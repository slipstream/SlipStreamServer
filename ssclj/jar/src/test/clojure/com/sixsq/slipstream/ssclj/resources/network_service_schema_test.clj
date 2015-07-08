(ns com.sixsq.slipstream.ssclj.resources.network-service-schema-test
  (:require
    [clojure.test                                         :refer :all]
    [com.sixsq.slipstream.ssclj.resources.network-service :refer :all]
    [com.sixsq.slipstream.ssclj.resources.test-utils      :as tu]))


(def acl {:owner {:type "USER"   :principal "john"}
          :rules [{:type "USER"  :principal "ANON" :right "VIEW"}]})

(def valid-firewall
  {
   :acl             acl
   :resourceURI     resource-uri

   :id              "NetWorkService/be23a1ba-0161-4a9a-b1e1-b2f4164e9a02"
   :created         "1970-04-16T08:30:00.0Z"
   :updated         "1970-04-16T19:30:00.0Z"

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
                          :port       {:tcp-range [30 56]}}

                        { :protocol   "ICMP"
                          :direction  "inbound"
                          :address    {:CIDR "192.168.12.33/32"}
                          :port       {:icmp {:type 5 :code 1}}}
                       ]
                     }
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

(defn firewall-with
  [[& keys] val]
  (-> valid-firewall
      (assoc-in keys val)))

(deftest firewall-without-rules-is-valid
  (tu/is-valid? (firewall-with [:policies :rules] []) NetworkServiceFirewall)
  (tu/is-valid? (assoc-in valid-firewall [:policies] {}) NetworkServiceFirewall))

(deftest schema-state-values
  (tu/is-invalid?   (firewall-with [:state] "OF THE ART") NetworkServiceFirewall)
  (let [valid-states ["CREATING" "STARTED" "STOPPED" "ERROR"]]
    (tu/are-valid?    (map #(firewall-with [:state] %) valid-states) NetworkServiceFirewall)))

(deftest schema-tcp-port
  (doseq [invalid-port-range [ [] [1] [1 2 3] ["1" 2] [1 "2"]]]
    (let [invalid-rule  { :protocol   "TCP"
                          :direction  "inbound"
                          :address    {:CIDR "192.168.0.0/24"}
                          :port       {:tcp-range invalid-port-range}}]
      (tu/is-invalid? (firewall-with [:policies :rules] [invalid-rule]) NetworkServiceFirewall))))

(deftest schema-icmp
  (let [rule  { :protocol   "TCP"
                :direction  "inbound"
                :address    {:CIDR "192.168.0.0/24"}
                :port       {:icmp {:type 8 :code 0}}}]
    (tu/is-valid? (firewall-with [:policies :rules] [rule]) NetworkServiceFirewall)))

;; TODO
;(deftest schema-type-values
;  (tu/is-invalid?   (assoc valid-firewall :type "blah") NetworkServiceFirewall)
;  (tu/is-valid?     valid-firewall              NetworkServiceFirewall)
;  (tu/is-valid?     valid-load-balancer         NetworkServiceFirewall)
;  (tu/is-valid?     valid-QoS                   NetworkServiceFirewall))

