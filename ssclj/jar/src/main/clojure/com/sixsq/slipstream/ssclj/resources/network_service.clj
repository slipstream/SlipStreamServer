(ns com.sixsq.slipstream.ssclj.resources.network-service
  (:require
    [clojure.tools.logging                                  :as log]
    [schema.core                                            :as sc]

    [com.sixsq.slipstream.ssclj.resources.common.authz      :as a]
    [com.sixsq.slipstream.ssclj.resources.common.schema     :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud   :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils      :as u]
    [com.sixsq.slipstream.ssclj.resources.common.crud       :as crud]))

(def ^:const resource-tag     :network-services)
(def ^:const resource-name    "NetworkService")
(def ^:const collection-name  "NetworkServiceCollection")

(def ^:const resource-uri     (str c/slipstream-schema-uri resource-name))
(def ^:const create-uri       (str resource-uri "Create"))
(def ^:const collection-uri   (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner { :type      "ROLE"
                              :principal "ADMIN" }
                     :rules [{:type      "ROLE"
                              :principal "USER"
                              :right     "MODIFY" }]})
;;
;; Schema
;;

;; Thanks to
;; http://blog.markhatton.co.uk/2011/03/15/regular-expressions-for-ip-addresses-cidr-ranges-and-hostnames/
;;
(def CIDR
  {:CIDR
  #"^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\/([0-9]|[1-2][0-9]|3[0-2]))$"})

(def SecurityGroupName
  {:security-group-name c/NonBlankString})

(def TCPRange {:tcp-range [(sc/one sc/Int "start") (sc/one sc/Int "end")]})
(def ICMP     {:icmp {:type sc/Num :code sc/Num}})

(def ^:private SecurityRule
  {:protocol  (sc/enum "TCP" "UDP" "ICMP")
   :direction (sc/enum "inbound" "outbound")
   :address   (sc/either CIDR SecurityGroupName)
   :port      (sc/either TCPRange ICMP)})

(def CommonAttributes
  {:state       (sc/enum "CREATING" "STARTED" "STOPPED" "ERROR")
   :type        (sc/enum "Firewall")
   :policies    {(sc/optional-key :rules) [SecurityRule]}})

(def ^:private NetworkServiceFirewallCreate
  (merge
    c/CreateAttrs
    c/AclAttr
    CommonAttributes))

(def NetworkServiceFirewall
  (merge
    c/CommonAttrs
    c/AclAttr
    CommonAttributes))

;; TODO other types to implement
;; All Types from CIMI Spec:
;; Load Balancer  QoS  Firewall  VPN  DHCP  DNS  NAT
;; Gateway  Layer4 Port Forwarding  IP Routing
;; Virtual Network Device  Other
;;
;; End of Schema
;;

;;
;; "Implementations" of multimethod declared in crud namespace
;;

(defmethod crud/validate resource-uri
  [resource]
  ((u/create-validation-fn NetworkServiceFirewall) resource))

(defmethod crud/validate create-uri
   [resource]
   ((u/create-validation-fn NetworkServiceFirewallCreate) resource))
;
;(defmethod crud/add-acl resource-uri
;  [resource request]
;  (a/add-acl resource request))
;
;;;
;;; Create
;;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [{:keys [body] :as request}]
  (let [body (-> body
                 (assoc :resourceURI create-uri)
                 crud/validate)]
    (log/info create-uri ": will add NetworkService " body)
    (add-impl (assoc request :body body))))