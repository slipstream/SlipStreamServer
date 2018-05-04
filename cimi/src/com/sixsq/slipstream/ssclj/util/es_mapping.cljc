(ns com.sixsq.slipstream.ssclj.util.es-mapping
  "Utility for converting clojure.spec definitions to Elasticsearch mappings."
  (:require
    [spec-tools.visitor :as visitor]
    [spec-tools.parse :as parse]
    [spec-tools.impl :as impl]))

;;Code borrowed from https://github.com/metosin/spec-tools/blob/master/src/spec_tools/json_schema.cljc

(defn- only-entry? [key a-map] (= [key] (keys a-map)))

(defn- simplify-all-of [spec]
  (let [subspecs (->> (:allOf spec) (remove empty?))]
    (cond
      (empty? subspecs) (dissoc spec :allOf)
      (and (= (count subspecs) 1) (only-entry? :allOf spec)) (first subspecs)
      :else (assoc spec :allOf subspecs))))



(defn- set-type-from-first-child
  [[child & _]]
  (cond
    (string? child) {:type "keyword"}
    (boolean? child) {:type "boolean"}
    (double? child) {:type "double"}
    (integer? child) {:type "long"}
    (int? child) {:type "integer"}
    (seq? child) {:type "nested"}
    :else {}))


(defn- spec-dispatch [dispatch _ _ _] dispatch)

(defmulti accept-spec spec-dispatch :default ::default)

(defn transform
  ([spec]
   (transform spec nil))
  ([spec options]
   (visitor/visit spec accept-spec options)))


(defmethod accept-spec 'com.sixsq.slipstream.ssclj.resources.common.utils/as-datetime [_ _ _ _] {:type   "date"
                                                                                                 :format "strict_date_optional_time||epoch_millis"})

(defmethod accept-spec 'com.sixsq.slipstream.ssclj.resources.common.utils/as-text [_ _ _ _] {:type   "text"})


;;
;; predicate list taken from https://github.com/clojure/clojure/blob/master/src/clj/clojure/spec/gen.clj
;;

; any? (one-of [(return nil) (any-printable)])
#_(defmethod accept-spec 'clojure.core/any? [_ _ _ _] {})

; some? (such-that some? (any-printable))
#_(defmethod accept-spec 'clojure.core/some? [_ _ _ _] {})

; number? (one-of [(large-integer) (double)])
(defmethod accept-spec 'clojure.core/number? [_ _ _ _] {:type "double"})

#_(defmethod accept-spec 'clojure.core/pos? [_ _ _ _] {:minimum 0 :exclusiveMinimum true})

#_(defmethod accept-spec 'clojure.core/neg? [_ _ _ _] {:maximum 0 :exclusiveMaximum true})

; integer? (large-integer)
(defmethod accept-spec 'clojure.core/integer? [_ _ _ _] {:type "long"})

; int? (large-integer)
(defmethod accept-spec 'clojure.core/int? [_ _ _ _] {:type "long"})

; pos-int? (large-integer* {:min 1})
(defmethod accept-spec 'clojure.core/pos-int? [_ _ _ _] {:type "long"})

; neg-int? (large-integer* {:max -1})
(defmethod accept-spec 'clojure.core/neg-int? [_ _ _ _] {:type "long"})

; nat-int? (large-integer* {:min 0})
(defmethod accept-spec 'clojure.core/nat-int? [_ _ _ _] {:type "long"})

; float? (double)
(defmethod accept-spec 'clojure.core/float? [_ _ _ _] {:type "float"})

; double? (double)
(defmethod accept-spec 'clojure.core/double? [_ _ _ _] {:type "double"})

; boolean? (boolean)
(defmethod accept-spec 'clojure.core/boolean? [_ _ _ _] {:type "boolean"})

; string? (string-alphanumeric)
(defmethod accept-spec 'clojure.core/string? [_ _ _ _] {:type "keyword"})

; ident? (one-of [(keyword-ns) (symbol-ns)])
(defmethod accept-spec 'clojure.core/ident? [_ _ _ _] {:type "keyword"})

; simple-ident? (one-of [(keyword) (symbol)])
(defmethod accept-spec 'clojure.core/simple-ident? [_ _ _ _] {:type "keyword"})

; qualified-ident? (such-that qualified? (one-of [(keyword-ns) (symbol-ns)]))
(defmethod accept-spec 'clojure.core/qualified-ident? [_ _ _ _] {:type "keyword"})

; keyword? (keyword-ns)
(defmethod accept-spec 'clojure.core/keyword? [_ _ _ _] {:type "keyword"})

; simple-keyword? (keyword)
(defmethod accept-spec 'clojure.core/simple-keyword? [_ _ _ _] {:type "keyword"})

; qualified-keyword? (such-that qualified? (keyword-ns))
(defmethod accept-spec 'clojure.core/qualified-keyword? [_ _ _ _] {:type "keyword"})

; symbol? (symbol-ns)
(defmethod accept-spec 'clojure.core/symbol? [_ _ _ _] {:type "keyword"})

; simple-symbol? (symbol)
(defmethod accept-spec 'clojure.core/simple-symbol? [_ _ _ _] {:type "keyword"})

; qualified-symbol? (such-that qualified? (symbol-ns))
(defmethod accept-spec 'clojure.core/qualified-symbol? [_ _ _ _] {:type "keyword"})

; uuid? (uuid)
(defmethod accept-spec 'clojure.core/uuid? [_ _ _ _] {:type "keyword"})

; uri? (fmap #(java.net.URI/create (str "http://" % ".com")) (uuid))
(defmethod accept-spec 'clojure.core/uri? [_ _ _ _] {:type "keyword"})

; bigdec? (fmap #(BigDecimal/valueOf %)
;               (double* {:infinite? false :NaN? false}))
(defmethod accept-spec 'clojure.core/decimal? [_ _ _ _] {:type "double"})

; inst? (fmap #(java.util.Date. %)
;             (large-integer))
(defmethod accept-spec 'clojure.core/inst? [_ _ _ _] {:type   "date"
                                                      :format "strict_date_optional_time||epoch_millis"})

; map? (map simple simple)
(defmethod accept-spec 'clojure.core/map? [_ _ _ _] {:type "object"})

; char? (char)
(defmethod accept-spec 'clojure.core/char? [_ _ _ _] {:type "keyword"})

; nil? (return nil)
(defmethod accept-spec 'clojure.core/nil? [_ _ _ _] {:type "null"})

; false? (return false)
(defmethod accept-spec 'clojure.core/false? [_ _ _ _] {:type "boolean"})

; true? (return true)
(defmethod accept-spec 'clojure.core/true? [_ _ _ _] {:type "boolean"})

; zero? (return 0)
(defmethod accept-spec 'clojure.core/zero? [_ _ _ _] {:type "integer"})

; coll? (one-of [(map simple simple)
;                (list simple)
;                (vector simple)
;                (set simple)])
(defmethod accept-spec 'clojure.core/coll? [_ _ _ _] {:type "object"})

; associative? (one-of [(map simple simple) (vector simple)])
(defmethod accept-spec 'clojure.core/associative? [_ _ _ _] {:type "object"})

; ratio? (such-that ratio? (ratio))
(defmethod accept-spec 'clojure.core/ratio? [_ _ _ _] {:type "integer"})

; bytes? (bytes)
(defmethod accept-spec 'clojure.core/bytes? [_ _ _ _] {:type "keyword"})

(defmethod accept-spec ::visitor/set [dispatch spec children _]
  ;;{:enum children}
  (set-type-from-first-child children))

#_(defmethod accept-spec 'clojure.core/set? [_ _ _ _] {})



(defmethod accept-spec 'clojure.spec.alpha/keys [_ spec children _]
  (let [{:keys [req req-un opt opt-un]} (impl/parse-keys (impl/extract-form spec))
        names-un (map name (concat req-un opt-un))
        names (map impl/qualified-name (concat req opt))]
    {:type       "object"
     :properties (zipmap (concat names names-un) children)}))

(defmethod accept-spec 'clojure.spec.alpha/or [_ _ children _]
  (first children))

(defmethod accept-spec 'clojure.spec.alpha/and [_ _ children _]
  (first children) #_(simplify-all-of {:allOf children}))

(defmethod accept-spec 'clojure.spec.alpha/merge [_ _ children _]
  {:type       "object"
   :properties (apply merge (map :properties children))})

(defmethod accept-spec 'clojure.spec.alpha/every [_ spec children _]
  (let [form (impl/extract-form spec)
        {:keys [type]} (parse/parse-spec form)]
    (case type
      :map {:type "object", :properties (impl/unwrap children)}
      :set (impl/unwrap children)
      :vector (impl/unwrap children))))

(defmethod accept-spec 'clojure.spec.alpha/every-kv [_ _ children _]
  {:type "object", :properties (second children)})

(defmethod accept-spec ::visitor/map-of [_ _ children _]
  {:type "object", :properties (second children)})

(defmethod accept-spec ::visitor/set-of [_ _ children _]
  (impl/unwrap children))

(defmethod accept-spec ::visitor/vector-of [_ _ children _]
  (impl/unwrap children))

(defmethod accept-spec 'clojure.spec.alpha/* [_ _ children _]
  (impl/unwrap children))

(defmethod accept-spec 'clojure.spec.alpha/+ [_ _ children _]
  (impl/unwrap children))

(defmethod accept-spec 'clojure.spec.alpha/? [_ _ children _]
  (impl/unwrap children))

;; NOTE: this will not be correct if the types of the children are different.
;; Such fields cannot be represented in an Elasticsearch mapping.
(defmethod accept-spec 'clojure.spec.alpha/alt [_ _ children _]
  (first children))

;; NOTE: this will not be correct if the types of the children are different.
;; Such fields cannot be represented in an Elasticsearch mapping.
(defmethod accept-spec 'clojure.spec.alpha/cat [_ _ children _]
  (first children))

; &

;; NOTE: this will not be correct if the types of the children are different.
;; Such fields cannot be represented in an Elasticsearch mapping.
(defmethod accept-spec 'clojure.spec.alpha/tuple [_ _ children _]
  (first children))

; keys*

;; NOTE: all fields in Elasticsearch are nilable, so this just uses the type
;; of the first child.
(defmethod accept-spec 'clojure.spec.alpha/nilable [_ _ children _]
  (impl/unwrap children))

;; this is just a function in clojure.spec?
(defmethod accept-spec 'clojure.spec.alpha/int-in-range? [_ _ _ _]
  {:type "long"})

(defmethod accept-spec ::visitor/spec [_ _ children _]
  (impl/unwrap children))

(defmethod accept-spec ::default [_ _ _ _]
  {})


(def default-mapping {:dynamic_templates [{:strings {:match              "*"
                                                     :match_mapping_type "string"
                                                     :mapping            {:type "keyword"}}}
                                          {:longs {:match              "*"
                                                   :match_mapping_type "long"
                                                   :mapping            {:type "long"}}}]})

(defn mapping
  [spec]
  {:mapping {:_doc (merge default-mapping {:mappings (transform spec)})}})
