(ns com.sixsq.slipstream.db.serializers.ServiceConfigSerializer
  (:require
    [clojure.pprint :refer [pprint]]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.es.es-util :as esu]
    [com.sixsq.slipstream.db.impl :as db])
  (:import
    [com.google.gson GsonBuilder]
    [com.sixsq.slipstream.persistence ServiceConfiguration])
  (:gen-class
    :methods [#^{:static true} [store [com.sixsq.slipstream.persistence.ServiceConfiguration] com.sixsq.slipstream.persistence.ServiceConfiguration]
              #^{:static true} [load [] com.sixsq.slipstream.persistence.ServiceConfiguration]]))

(def acl
  {:owner {:principal "ADMIN"
           :type      "ROLE"}
   :rules [{:principal "ADMIN"
            :type      "ROLE"
            :right     "VIEW"}]}
  )

(defn -store
  [^ServiceConfiguration sc]
  (let [id (.getId sc)
        _ (println "ID .... " id)
        gson (-> (GsonBuilder.)
                 (.setPrettyPrinting)
                 (.enableComplexMapKeySerialization)
                 (.excludeFieldsWithoutExposeAnnotation)
                 (.create))
        doc (.toJson gson sc)
        data (esb/doc->data doc)]
    (println doc)
    (pprint data)
    (db/add "configuration" (assoc data :id "configuration") acl)
    (println (esu/dump esb/*client* esb/index-name "configuration"))
    )
  sc)

(defn -load
  []
  (pprint (db/retrieve "configuration" {:user-name "konstan" :user-roles ["ADMIN"]}))
  (ServiceConfiguration.))
