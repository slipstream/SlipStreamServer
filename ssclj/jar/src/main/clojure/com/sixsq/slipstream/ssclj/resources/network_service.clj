(ns com.sixsq.slipstream.ssclj.resources.network-service
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [schema.core                                        :as sc]))

(def ^:const resource-tag     :network-services)
(def ^:const resource-name    "NetworkService")
(def ^:const collection-name  "NetworkServiceCollection")

(def ^:const resource-uri     (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri   (str c/slipstream-schema-uri collection-name))

(def ^:private NetworkServiceCommon
  (merge
    c/CreateAttrs
    c/AclAttr
    {:id    c/NonBlankString
     :state (sc/enum "CREATING" "STARTED" "STOPPED" "ERROR") }))

(def ^:private SecurityRule
  {:protocol (sc/enum "TCP" "UDP" "ICMP")})

(def ^:private NetworkServiceFirewall
  (merge
    NetworkServiceCommon
    {:type      (sc/enum "Firewall")
     :policies [SecurityRule]}))

(def ^:private NetworkServiceLoadBalancer
  (merge
    NetworkServiceCommon
    {:type      (sc/enum "Load Balancer")
     :policies {}}))

(def ^:private NetworkServiceQos
  (merge
    NetworkServiceCommon
    {:type      (sc/enum "QoS")
     :policies {}}))

(def NetworkService
  (sc/either
    NetworkServiceFirewall
    NetworkServiceLoadBalancer
    NetworkServiceQos))

;;  c/CreateAttrs
;;  c/AclAttr
;;  {:id    c/NonBlankString
;;   :state (sc/enum "CREATING" "STARTED" "STOPPED" "ERROR")
;;   :type  (sc/enum "Load Balancer" "QoS" "Firewall" "VPN" "DHCP" "DNS" "NAT"
;;                   "Gateway" "Layer4 Port Forwarding" "IP Routing"
;;                   "Virtual Network Device" "Other")}))
