(ns 
  com.sixsq.slipstream.ssclj.resources.usage-summary
  (:require
    [clojure.tools.logging :as log]
    [schema.core :as s]
    [korma.core :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.authz :as a]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    [com.sixsq.slipstream.ssclj.resources.common.std-crud :as std-crud]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]))

(def ^:const resource-tag :events)
(def ^:const resource-name "UsageSummary")
(def ^:const collection-name "UsageSummaryCollection")

(def ^:const resource-uri (str c/slipstream-schema-uri resource-name))
(def ^:const collection-uri (str c/slipstream-schema-uri collection-name))

(def collection-acl {:owner {:principal "ADMIN"
                             :type      "ROLE"}
                     :rules [{:principal "ANON"
                              :type      "ROLE"
                              :right     "VIEW"}]})

(defentity usage-summaries)

;;
;; collection
;;

(defmethod crud/query resource-name
           [request]
  (select usage-summaries))