(ns com.sixsq.slipstream.ssclj.util.spec
  "Utilities that provide common spec definition patterns that aren't
   supported directly by the core spec functions and macros."
  (:require
    [clojure.set :as set]
    [clojure.spec.alpha :as s]
    [com.sixsq.slipstream.db.es.common.es-mapping :as es-mapping]
    [spec-tools.impl :as impl]
    [spec-tools.parse :as st-parse]
    [spec-tools.visitor :as visitor]
    [clojure.tools.logging :as log]))

(def ^:private all-ascii-chars (map str (map char (range 0 256))))

(defn- regex-chars
  "Provides a list of ASCII characters that satisfy the regex pattern."
  [pattern]
  (set (filter #(re-matches pattern %) all-ascii-chars)))

(defn merge-kw-lists
  "Merges two lists (or seqs) of namespaced keywords. The results will
   be a sorted vector with duplicates removed."
  [kws1 kws2]
  (vec (sort (set/union (set kws1) (set kws2)))))

(defn merge-keys-specs
  "Merges the given clojure.spec/keys specs provided as a list of maps.
   All the arguments are eval'ed and must evaluate to map constants."
  [map-specs]
  (->> map-specs
       (map eval)
       (apply merge-with merge-kw-lists)))

(defmacro only-keys
  "Creates a closed map definition where only the defined keys are
   permitted. The arguments must be literals, using the same function
   signature as clojure.spec/keys.  (This implementation was provided
   on the clojure mailing list by Alistair Roche.)"
  [& {:keys [req req-un opt opt-un] :as args}]
  `(s/merge (s/keys ~@(apply concat (vec args)))
            (s/map-of ~(set (concat req
                                    (map (comp keyword name) req-un)
                                    opt
                                    (map (comp keyword name) opt-un)))
                      any?)))

(defmacro only-keys-maps
  "Creates a closed map definition from one or more maps that contain
   key specifications as for clojure.spec/keys. All of the arguments
   are eval'ed, so they may be vars containing the definition(s). All
   of the arguments must evaluate to compile-time map constants."
  [& map-specs]
  (let [{:keys [req req-un opt opt-un] :as map-spec} (merge-keys-specs map-specs)]
    `(s/merge (s/keys ~@(apply concat (vec map-spec)))
              (s/map-of ~(set (concat req
                                      (map (comp keyword name) req-un)
                                      opt
                                      (map (comp keyword name) opt-un)))
                        any?))))

(defmacro constrained-map
  "Creates an open map spec using the supplied keys specs with the
   additional constraint that all unspecified entries must match the
   given key and value specs. The keys specs will be evaluated."
  [key-spec value-spec & map-specs]
  (let [{:keys [req req-un opt opt-un] :as map-spec} (merge-keys-specs map-specs)]
    `(s/merge
       (s/every (s/or :attrs (s/tuple ~(set (concat req
                                                    (map (comp keyword name) req-un)
                                                    opt
                                                    (map (comp keyword name) opt-un)))
                                      any?)
                      :link (s/tuple ~key-spec ~value-spec)))
       (s/keys ~@(apply concat (vec map-spec))))))


;;
;; spec parsing patches
;;

(defmethod st-parse/parse-form 'com.sixsq.slipstream.ssclj.util.spec/only-keys [dispatch form]
  (log/error "FORM PARSE" form)

  (st-parse/parse-form 'clojure.spec.alpha/keys form))


(defn transform-form
  [[_ & keys-specs]]
  (concat ['clojure.spec.alpha/keys] (merge-keys-specs keys-specs)))


(defmethod st-parse/parse-form 'com.sixsq.slipstream.ssclj.util.spec/only-keys-maps [dispatch form]
  (log/error "FORM PARSE" form)
  (log/error "XFORM" (transform-form form))
  (let [spec-name (first form)
        xformed-form (transform-form form)
        arg (concat ['clojure.spec.alpha/keys] [spec-name] xformed-form)]
    (log/error "ARG" arg)
    (apply st-parse/parse-form arg)))


(defmethod st-parse/parse-form 'com.sixsq.slipstream.ssclj.util.spec/constrained-map [dispatch form]
  (st-parse/parse-form 'clojure.spec.alpha/keys form))

;;
;; patches for spec walking via visitor
;;

(defmethod visitor/visit-spec 'com.sixsq.slipstream.ssclj.util.spec/only-keys [spec accept options]
  (let [keys (impl/extract-keys (impl/extract-form spec))]
    (accept 'clojure.spec.alpha/keys spec (mapv #(visitor/visit % accept options) keys) options)))


(defmethod visitor/visit-spec 'com.sixsq.slipstream.ssclj.util.spec/only-keys-maps [spec accept options]
  (log/error spec)
  (log/error (impl/extract-form spec))
  #_(log/error (impl/extract-keys (impl/extract-form spec)))
  #_(let [keys (impl/extract-keys (impl/extract-form spec))]
    (log/error spec)
    (log/error (impl/extract-form spec))
    (log/error (impl/extract-keys (impl/extract-form spec)))
    (accept 'com.sixsq.slipstream.ssclj.util.spec/only-keys-maps spec (mapv #(visitor/visit % accept options) keys) options)))


(defmethod visitor/visit-spec 'com.sixsq.slipstream.ssclj.util.spec/constrained-map [spec accept options]
  (let [keys (impl/extract-keys (impl/extract-form spec))]
    (accept 'com.sixsq.slipstream.ssclj.util.spec/constrained-map spec (mapv #(visitor/visit % accept options) keys) options)))

;;
;; patch transform of spec into ES mapping
;;

(defmethod es-mapping/accept-spec 'com.sixsq.slipstream.ssclj.util.spec/only-keys [dispatch spec children arg]
  (es-mapping/accept-spec 'clojure.spec.alpha/keys spec children arg))


(defmethod es-mapping/accept-spec 'com.sixsq.slipstream.ssclj.util.spec/only-keys-maps [dispatch spec children arg]
  (es-mapping/accept-spec 'clojure.spec.alpha/keys spec children arg))


(defmethod es-mapping/accept-spec 'com.sixsq.slipstream.ssclj.util.spec/constrained-map [dispatch spec children arg]
  (es-mapping/accept-spec 'clojure.spec.alpha/keys spec children arg))
