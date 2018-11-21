(ns com.sixsq.slipstream.ssclj.resources.service-offer
  "
The ServiceOffer resource is the primary resource in the Service Catalog. Each
offer describes a specific resource offer from a cloud service provider. The
specific offer for a virtual machine would typically describe the offer's CPU,
RAM, and disk resources, the geographical location, and the price.

In the example offer from the Open Telekom Cloud (OTC), you can see three
types of information: metadata (e.g. id, name), the cloud connector (in
connector/href), and the cloud characteristics (e.g. price:unitCost,
resource:country).

The schema for the cloud characteristics is open, allowing cloud providers to
supply any information that is useful for users. The only requirement is that
these attributes must be namespaced. The namespaces **must** be defined in a
ServiceAttributeNamespace resources and the attribute itself **may** be
described in a ServiceAttribute resource.

Currently, only SlipStream administrators can add, update, or delete
ServiceOffer resources. The standard CIMI patterns for these actions apply to
these resources. Most users will search the ServiceOffer entries by using a
filter expression on the ServiceOffer collection. The example searches for all
ServiceOffers from the 'exoscale-ch-gva' cloud with more than 4096 MB of RAM.


An example service offer from the Open Telekom Cloud (OTC).

```json
{
  \"id\" : \"service-offer/0b2a46dd-f8c8-4420-a851-519daa581a1d\",
  \"name\" : \"(4/4096/800 c1.xlarge windows) [DE]\",
  \"description\" : \"VM (standard) with 4 vCPU, 4096 MiB RAM, 800 GiB root disk, windows [DE] (c1.xlarge)\",
  \"created\" : \"2017-06-26T11:13:42.883Z\",
  \"updated\" : \"2017-07-05T15:17:55.005Z\",

  \"connector\" : {
    \"href\" : \"open-telekom-de1\"
  },

  \"price:billingPeriodCode\" : \"MIN\",
  \"price:billingUnitCode\" : \"HUR\",
  \"price:currency\" : \"EUR\",
  \"price:freeUnits\" : 0,
  \"price:unitCost\" : 0.38711111111111113,
  \"price:unitCode\" : \"C62\",

  \"resource:class\" : \"standard\",
  \"resource:country\" : \"DE\",
  \"resource:diskType\" : \"SATA\",
  \"resource:instanceType\" : \"c1.xlarge\",
  \"resource:operatingSystem\" : \"windows\",
  \"resource:platform\" : \"openstack\",
  \"resource:ram\" : 4096,
  \"resource:vcpu\" : 4,

  \"otc:instanceType\" : \"c1.xlarge\",
  \"otc:flavorPurpose\" : \"Compute I Nr. 3\",

  \"acl\" : {\"...\" : \"...\"}
}
```

To show all of the ServiceOffer resources from the 'exoscale-ch-gva' cloud
that provide more than 4096 MB of RAM. Be sure to escape the dollar sign in the
`$filter` query parameter!

```shell
curl 'https://nuv.la/api/service-offer?$filter=connector/href=\"exoscale-ch-gva\" and resource:ram>4096'
```
"
  (:require
    [com.sixsq.slipstream.auth.acl :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.service-attribute-namespace :as sn]
    [com.sixsq.slipstream.ssclj.resources.spec.service-offer :as so]
    [com.sixsq.slipstream.util.response :as sr]
    [ring.util.response :as r]
    [superstring.core :as str]))

(def ^:const resource-name "ServiceOffer")

(def ^:const resource-tag (keyword (str (str/camel-case resource-name) "s")))

(def ^:const resource-url (u/de-camelcase resource-name))

(def ^:const collection-name "ServiceOfferCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))

(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "USER"
                              :type      "ROLE"
                              :right     "MODIFY"}]})

;;
;; multimethods for validation and operations
;;

(defn valid-attribute-name?
  [valid-prefixes attr-name]
  (let [[ns _] (str/split (name attr-name) #":")]
    (valid-prefixes ns)))

(defn- valid-attributes?
  [validator resource]
  (if-not (map? resource)
    true
    (and (every? validator (keys resource))
         (every? (partial valid-attributes? validator) (vals resource)))))

(defn- throw-wrong-namespace
  []
  (let [code 406
        msg "resource attributes do not satisfy defined namespaces"
        response (-> {:status code :message msg}
                     sr/json-response
                     (r/status code))]
    (throw (ex-info msg response))))

(defn- validate-attributes
  [resource]
  (let [valid-prefixes (sn/all-prefixes)
        resource-payload (dissoc resource :acl :id :resourceURI :name :description
                                 :created :updated :properties :operations :connector)
        validator (partial valid-attribute-name? valid-prefixes)]
    (if (valid-attributes? validator resource-payload)
      resource
      (throw-wrong-namespace))))

(def validate-fn (u/create-spec-validation-fn ::so/service-offer))
(defmethod crud/validate resource-uri
  [resource]
  (-> resource
      validate-fn
      validate-attributes))

(defmethod crud/add-acl resource-uri
  [resource request]
  (a/add-acl resource request))

;;
;; CRUD operations
;;

(def add-impl (std-crud/add-fn resource-name collection-acl resource-uri))

(defmethod crud/add resource-name
  [request]
  (add-impl request))

(def retrieve-impl (std-crud/retrieve-fn resource-name))

(defmethod crud/retrieve resource-name
  [request]
  (retrieve-impl request))

(def edit-impl (std-crud/edit-fn resource-name))

(defmethod crud/edit resource-name
  [request]
  (edit-impl request))

(def delete-impl (std-crud/delete-fn resource-name))

(defmethod crud/delete resource-name
  [request]
  (delete-impl request))

(def query-impl (std-crud/query-fn resource-name collection-acl collection-uri resource-tag))

(defmethod crud/query resource-name
  [request]
  (query-impl request))


;;
;; initialization
;;
(defn initialize
  []
  (std-crud/initialize resource-url ::so/service-offer))
