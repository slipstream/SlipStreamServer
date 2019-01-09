(ns com.sixsq.slipstream.ssclj.resources.spec.deployment
  (:require
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as cimi-common]
    [com.sixsq.slipstream.ssclj.resources.spec.core :as cimi-core]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template :as deployment-template]
    [com.sixsq.slipstream.ssclj.util.spec :as su]))


(s/def ::state #{"CREATED",
                 "STARTING", "STARTED",
                 "STOPPING", "STOPPED",
                 "PAUSING", "PAUSED",
                 "SUSPENDING", "SUSPENDED",
                 "ERROR"})

(s/def ::module ::cimi-common/resource-link)

(def ^:const credential-href-regex #"^credential/[a-z0-9]+(-[a-z0-9]+)*(_\d+)?$")
(s/def ::href (s/and string? #(re-matches credential-href-regex %)))
(s/def ::secret string?)
(s/def ::clientAPIKey (su/only-keys :req-un [::href
                                             ::secret]))

(s/def ::sshPublicKeys (s/coll-of ::cimi-core/nonblank-string :min-count 1 :kind vector?))

(s/def ::deploymentTemplate ::cimi-common/resource-link)


(def ^:const external-object-id-regex #"^external-object/[a-z0-9]+(-[a-z0-9]+)*(_\d+)?$")
(defn external-object-id? [s] (re-matches external-object-id-regex s))

(s/def ::external-object-id (s/and string? external-object-id?))
(s/def ::externalObjects (s/coll-of ::external-object-id :min-count 1 :kind vector?))


(def ^:const service-offer-id-regex #"^service-offer/[a-z0-9]+(-[a-z0-9]+)*(_\d+)?$")
(defn service-offer-id? [s] (re-matches service-offer-id-regex s))
(defn service-offer-id-keyword? [s] (-> s symbol str service-offer-id?))

(s/def ::service-offer-id (s/and string? service-offer-id?))
(s/def ::service-offer-id-keyword (s/and keyword? service-offer-id-keyword?))
(s/def ::data-set-ids (s/nilable (s/coll-of ::service-offer-id :min-count 1 :kind vector?)))
(s/def ::serviceOffers (s/map-of ::service-offer-id-keyword ::data-set-ids :min-count 1))


(def deployment-keys-spec
  (su/merge-keys-specs [cimi-common/common-attrs
                        deployment-template/deployment-template-keys-spec
                        {:req-un [::state
                                  ::clientAPIKey]
                         :opt-un [::deploymentTemplate
                                  ::sshPublicKeys
                                  ::externalObjects
                                  ::serviceOffers]}]))

(s/def ::deployment (su/only-keys-maps deployment-keys-spec))
