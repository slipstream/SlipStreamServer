(ns com.sixsq.slipstream.db.serializers.test-utils
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [clojure.data.xml :as xml]
    [clojure.java.io :as io]
    [com.sixsq.slipstream.ssclj.resources.configuration :as cr]
    [com.sixsq.slipstream.db.es.es-binding :as esb]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.serializers.ServiceConfigSerializer :as scs])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    [com.sixsq.slipstream.persistence ParameterType]
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter]
    ))

(def xml-conf (.getPath (io/resource "configuration.xml")))

(defn xml-params
  [xml]
  (-> xml :content first :content))

(defn xml-param-attrs
  [p]
  (-> p :content second :attrs))

(defn xml-param-elems
  [p]
  (-> p :content second :content))

(defn xml-param-value
  [p]
  (let [pelems (xml-param-elems p)]
    (first (for [e pelems :when (= (:tag e) :value)]
             (-> e :content first)))))

(defn xml-param-instructions
  [p]
  (let [pelems (xml-param-elems p)]
    (first (for [e pelems :when (= (:tag e) :instructions)]
             (-> e :content first)))))

(defn xml-param-enums
  [content]
  (first (for [e content :when (= (:tag e) :enumValues)] e)))

(defn xml-param-enum-values
  [p]
  (if-let [enums (-> p xml-param-elems xml-param-enums)]
    (for [v (-> enums :content)]
      (-> v :content first))
    '()))

(defn xml-params-parse
  "[[{attributes} \"value\" \"instruction\" '(enumValues)] ..]"
  [xml]
  (map #(identity [(xml-param-attrs %) (xml-param-value %) (xml-param-instructions %) (xml-param-enum-values %)])
       (xml-params xml)))

(defn xml-param-global?
  [p]
  (contains? scs/global-categories (:category p)))

(defn- print-param-parsed-from-xml
  [attrs value instructions enum-vals]
  (print "PA ") (pprint attrs)
  (println "PV " value)
  (println "PI " instructions)
  (println "PEV " enum-vals))

(defn conf-xml->sc
  []
  (let [xml-data (-> xml-conf slurp xml/parse-str)
        sc (ServiceConfiguration.)]
    (doseq [[attrs value instructions enum-vals] (xml-params-parse xml-data)]

      ;(print-param-parsed-from-xml attrs value instructions enum-vals)

      (let [scp (ServiceConfigurationParameter. (:name attrs) value (:description attrs))]
        (.setCategory scp (:category attrs))
        (.setMandatory scp (read-string (:mandatory attrs)))
        (.setType scp (ParameterType/valueOf (:type attrs)))
        (.setReadonly scp (read-string (:readonly attrs)))
        (.setOrder scp (read-string (:order_ attrs)))
        (if instructions (.setInstructions scp instructions))
        (if-not (empty? enum-vals) (.setEnumValues scp enum-vals))
        (.setParameter sc scp)))
    sc))

(defn create-es-client
  []
  (db/set-impl! (esb/get-instance))
  (esb/set-client! (esb/create-test-client)))

(defn db-add-default-config
  []
  (-> cr/default-configuration
      scs/as-request
      cr/add-impl))

;; Fixtures.
(defn fixture-start-es-db
  [f]
  (create-es-client)
  (f))


