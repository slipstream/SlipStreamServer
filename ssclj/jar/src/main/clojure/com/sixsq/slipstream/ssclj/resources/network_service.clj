(ns com.sixsq.slipstream.ssclj.resources.network-service
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [schema.core                                        :as sc]))

(def ^:const resource-tag     :network-services)
(def ^:const resource-name    "NetworkService")
(def ^:const collection-name  "NetworkServiceCollection")

(def ^:const resource-uri     (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri   (str c/slipstream-schema-uri collection-name))

;; Thanks to
;; http://blog.markhatton.co.uk/2011/03/15/regular-expressions-for-ip-addresses-cidr-ranges-and-hostnames/
(def CIDR
  {:CIDR
  #"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$"})

(def SecurityGroupName
  {:security-group-name c/NonBlankString})

(def TCPRange {:tcp-range [c/Numeric c/Numeric]})

(def ^:private NetworkServiceCommon
  (merge
    c/CreateAttrs
    c/AclAttr
    {:id    c/NonBlankString
     :state (sc/enum "CREATING" "STARTED" "STOPPED" "ERROR") }))

(def ^:private SecurityRule
  {:protocol  (sc/enum "TCP" "UDP" "ICMP")
   :direction (sc/enum "inbound" "outbound")
   :address   (sc/either CIDR SecurityGroupName)
   :port      TCPRange})

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


