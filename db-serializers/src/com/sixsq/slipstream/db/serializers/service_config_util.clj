(ns com.sixsq.slipstream.db.serializers.service-config-util
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.test :refer :all]
    [clojure.data.xml :as xml]
    [me.raynes.fs :as fs]
    [com.sixsq.slipstream.db.serializers.utils :as u])
  (:import
    [com.sixsq.slipstream.persistence ServiceConfiguration]))

(defn- xml-params
  [xml]
  (-> xml :content first :content))

(defn- xml-param-elems
  [p]
  (-> p :content second :content))

(defn- filter-by-tag
  [tag vals]
  (filter #(= (:tag %) tag) vals))

(defn xml-param
  [p tag]
  (->> p
       xml-param-elems
       (filter-by-tag tag)
       first
       :content
       first))

(defn xml-param-attrs
  [p]
  (-> p :content second :attrs))

(defn xml-param-value
  [p]
  (xml-param p :value))

(defn xml-param-instructions
  [p]
  (xml-param p :instructions))

(defn xml-param-enums
  [content]
  (->> content
       (filter-by-tag :enumValues)
       first))

(defn xml-param-enum-values
  [p]
  (if-let [enums (-> p xml-param-elems xml-param-enums)]
    (map #(-> % :content first) (:content enums))
    []))

(defn xml-params-parse
  "Returns [[{attributes} \"value\" \"instruction\" [enumValues]] ..]"
  [xml]
  (map (juxt xml-param-attrs xml-param-value xml-param-instructions xml-param-enum-values) (xml-params xml)))

(defn conf-xml->sc
  "xml-conf - SlipStream service configuration as XML string."
  [xml-conf]
  (let [xml-data (xml/parse-str xml-conf)
        sc       (ServiceConfiguration.)]
    (doseq [[attrs value instructions enum-vals] (xml-params-parse xml-data)]
      (let [desc (merge attrs {:instructions instructions
                               :enum         enum-vals})]
        (.setParameter sc (u/build-sc-param value desc))))
    sc))

(defn sc-get-param-value
  "sc - ServiceConfiguration
  pname - str (parameter name)"
  [sc pname]
  (if-let [p (.getParameter sc pname)]
      (.getValue p)))

(defn spit-pprint
  [obj fpath]
  (let [f (fs/expand-home fpath)]
    (with-open [^java.io.Writer w (apply clojure.java.io/writer f {})]
      (clojure.pprint/pprint obj w))))

