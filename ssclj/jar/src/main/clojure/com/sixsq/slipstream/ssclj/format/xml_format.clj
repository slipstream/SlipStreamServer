(ns com.sixsq.slipstream.ssclj.format.xml-format
  (:refer-clojure :exclude [format])
  (require
    [clojure.data.xml :as xml]
    [clojure.walk :refer [walk]]))

(defn split-resource-uri
  "Splits the given resource URI into the local name and the parent
  URI.  If the string does not match the URI pattern, then nil is
  returned."
  [s]
  (when s
    (if-let [matches (re-matches #"^(.*)/(.*)$" s)]
      (rest matches))))

(defn property-element
  "A property element has its key as the 'key' attribute and the
  value in the content of the element."
  [[k v]]
  (xml/element "property" {:key k} v))

(defn operation-element
  "An operation element is empty with two attributes: rel and href."
  [{:keys [rel href]}]
  (xml/element "operation" {:rel rel :href href}))

(defmulti cimi->xml
          "Converts a clojure data structure representing a CIMI
          resource into its XML representation.  In general, maps
          are elements with children and lists are flattened into
          a sequence of elements.  However, there are several
          notable exceptions that this method must treat specially,
          including properties, operations, and references."
          (fn
            [k form]
            (cond
              (:resourceURI form) :resource
              (= :properties k) :properties
              (= :operations k) :operations
              (:href form) :reference
              (map? form) :map
              :else :default)))

(defmethod cimi->xml :resource
  [_ {:keys [resourceURI] :as form}]
  (let [[xmlns tag] (split-resource-uri resourceURI)]
    (xml/element tag
                 {:xmlns xmlns}
                 (cimi->xml nil (dissoc form :resourceURI)))))

(defmethod cimi->xml :properties
  [_ form]
  (map property-element form))

(defmethod cimi->xml :operations
  [_ form]
  (map operation-element form))

(defmethod cimi->xml :reference
  [k {:keys [href] :as form}]
  (xml/element k
               {:href href}
               (cimi->xml nil (dissoc form :href))))

(defmethod cimi->xml :map
  [_ form]
  (map (fn [[k v]] (cimi->xml k v)) form))

(defmethod cimi->xml :default
  [k v]
  (xml/element k {} v))


(defn xml->cimi
  "Converts the XML representation of a CIMI resource into a clojure
  data structure."
  [s]
  (let [xml (xml/parse-str s)]
    (println xml)))
