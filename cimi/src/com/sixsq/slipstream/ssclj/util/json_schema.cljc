(ns com.sixsq.slipstream.ssclj.util.json-schema
  "Tools for converting specs into JSON Schema."
  (:require [spec-tools.visitor :as visitor]
            [spec-tools.parse :as parse]
            [spec-tools.impl :as impl]
            [clojure.set :as set]
            [spec-tools.core :as st]))


;;Code borrowed from https://github.com/metosin/spec-tools/blob/master/src/spec_tools/json_schema.cljc

(defn- only-entry? [key a-map] (= [key] (keys a-map)))

(defn- simplify-all-of [spec]
  (let [subspecs (->> (:allOf spec) (remove empty?))]
    (cond
      (empty? subspecs) (dissoc spec :allOf)
      (and (= (count subspecs) 1) (only-entry? :allOf spec)) (first subspecs)
      :else (assoc spec :allOf subspecs))))



(defn- set-type-from-first-child
  [children]
  (let [child (first children)]
    (when child
      (cond
        (string? child) {:type "keyword"}
        (boolean? child){:type "boolean"}
        (double? child) {:type "double"}
        (integer? child) {:type "long"}
        (int? child) {:type "integer"}
        (seq? child) {:type "nested"}
        :else {}
        ))))


(defn- spec-dispatch [dispatch _ _ _] dispatch)

(defmulti accept-spec spec-dispatch :default ::default)

(defn transform
  ([spec]
   (transform spec nil))
  ([spec options]
   (visitor/visit spec accept-spec options)))

;;
;; predicate list taken from https://github.com/clojure/clojure/blob/master/src/clj/clojure/spec/gen.clj
;;

; any? (one-of [(return nil) (any-printable)])
(defmethod accept-spec 'clojure.core/any? [_ _ _ _] {})

; some? (such-that some? (any-printable))
(defmethod accept-spec 'clojure.core/some? [_ _ _ _] {})

; number? (one-of [(large-integer) (double)])
(defmethod accept-spec 'clojure.core/number? [_ _ _ _] {:type "double"})

(defmethod accept-spec 'clojure.core/pos? [_ _ _ _] {:minimum 0 :exclusiveMinimum true})

(defmethod accept-spec 'clojure.core/neg? [_ _ _ _] {:maximum 0 :exclusiveMaximum true})

; integer? (large-integer)
(defmethod accept-spec 'clojure.core/integer? [_ _ _ _] {:type "long"})

; int? (large-integer)
(defmethod accept-spec 'clojure.core/int? [_ _ _ _] {:type "integer"})

; pos-int? (large-integer* {:min 1})
(defmethod accept-spec 'clojure.core/pos-int? [_ _ _ _] {:type "integer"})

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
(defmethod accept-spec 'clojure.core/inst? [_ _ _ _] {:type "keyword"})

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
(defmethod accept-spec 'clojure.core/ratio? [_ _ _ _] {:type "keyword"})

(defmethod accept-spec ::visitor/set [dispatch spec children _]
  ;;{:enum children}
  (set-type-from-first-child children)
  )

(defmethod accept-spec 'clojure.core/set? [_ _ _ _] {:type "array" :uniqueItems true})



(defmethod accept-spec 'clojure.spec.alpha/keys [_ spec children _]
  (let [{:keys [req req-un opt opt-un]} (impl/parse-keys (impl/extract-form spec))
        names-un (map name (concat req-un opt-un))
        names (map impl/qualified-name (concat req opt))]
        {:type       "object"
         :properties (zipmap (concat names names-un) children)}))

(defmethod accept-spec 'clojure.spec.alpha/or [_ _ children _]
  {:anyOf children})

(defmethod accept-spec 'clojure.spec.alpha/and [_ _ children _]
  (simplify-all-of {:allOf children}))

(defmethod accept-spec 'clojure.spec.alpha/merge [_ _ children _]
  {:type       "object"
   :properties (apply merge (map :properties children))
   })

(defmethod accept-spec 'clojure.spec.alpha/every [_ spec children _]
  (let [form (impl/extract-form spec)
        {:keys [type]} (parse/parse-spec form)]
    (case type
      :map (maybe-with-title {:type "object", :additionalProperties (impl/unwrap children)} spec)
      :set {:type "array", :uniqueItems true, :items (impl/unwrap children)}
      :vector {:type "array", :items (impl/unwrap children)})))

(defmethod accept-spec 'clojure.spec.alpha/every-kv [_ spec children _]
  (maybe-with-title {:type "object", :additionalProperties (second children)} spec))

(defmethod accept-spec ::visitor/map-of [_ spec children _]
  (maybe-with-title {:type "object", :additionalProperties (second children)} spec))

(defmethod accept-spec ::visitor/set-of [_ _ children _]
  {:type "array", :items (impl/unwrap children), :uniqueItems true})

(defmethod accept-spec ::visitor/vector-of [_ _ children _]
  {:type "array", :items (impl/unwrap children)})

(defmethod accept-spec 'clojure.spec.alpha/* [_ _ children _]
  {:type "array" :items (impl/unwrap children)})

(defmethod accept-spec 'clojure.spec.alpha/+ [_ _ children _]
  {:type "array" :items (impl/unwrap children) :minItems 1})

(defmethod accept-spec 'clojure.spec.alpha/? [_ _ children _]
  {:type "array" :items (impl/unwrap children) :minItems 0})

(defmethod accept-spec 'clojure.spec.alpha/alt [_ _ children _]
  {:anyOf children})

(defmethod accept-spec 'clojure.spec.alpha/cat [_ _ children _]
  {:type  "array"
   :items {:anyOf children}})

; &

(defmethod accept-spec 'clojure.spec.alpha/tuple [_ _ children _]
  {:type  "array"
   :items children})

; keys*

(defmethod accept-spec 'clojure.spec.alpha/nilable [_ _ children _]
  {:oneOf [(impl/unwrap children) {:type "null"}]})

;; this is just a function in clojure.spec?
(defmethod accept-spec 'clojure.spec.alpha/int-in-range? [_ spec _ _]
  (let [[_ minimum maximum _] (impl/strip-fn-if-needed spec)]
    {:minimum minimum :maximum maximum}))

(defmethod accept-spec ::visitor/spec [_ spec children _]
  (let [[_ data] (impl/extract-form spec)]
    (impl/unwrap children)))

(defmethod accept-spec ::default [_ _ _ _]
  {})