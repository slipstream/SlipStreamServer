(ns com.sixsq.slipstream.db.serializers.test-utils
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [clojure.data.xml :as xml]
    [clojure.java.io :as io]
    [com.sixsq.slipstream.db.serializers.utils :as u])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]
    [com.sixsq.slipstream.persistence ParameterType]
    [com.sixsq.slipstream.persistence ServiceConfigurationParameter]))


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


;; Fixtures.
(defn fixture-start-es-db
  [f]
  (u/es-test-db-and-client)
  (f))

