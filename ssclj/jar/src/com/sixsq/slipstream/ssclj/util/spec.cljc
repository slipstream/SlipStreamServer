(ns com.sixsq.slipstream.ssclj.util.spec
  "Utilities that provide common spec definition patterns that aren't
   supported directly by the core spec functions and macros."
  (:require
    [clojure.set :as set]
    [clojure.spec :as s]))

(defn unnamespaced-kws
  "Removes the namespaces from the provided list of keywords
   and returns the resulting set."
  [kws]
  (set (map (comp keyword name) kws)))

(defn allowed-keys
  "Returns a set of all the allowed keys from a clojure.spec/keys
   specification provided as a map."
  [{:keys [req req-un opt opt-un]}]
  (set (concat req
               (unnamespaced-kws req-un)
               opt
               (unnamespaced-kws opt-un))))

(defmacro only-keys
  "Creates a closed map definition where only the defined keys are
   permitted. The arguments must be literals, using the same function
   signature as clojure.spec/keys.  (This implementation was provided
   on the clojure mailing list by Alistair Roche.)"
  [& {:as kw-args}]
  `(s/merge (s/keys ~@(apply concat (vec kw-args)))
            (s/map-of ~(allowed-keys kw-args) any?)))

(defmacro only-keys-maps
  "Creates a closed map definition from one or more maps that contain
   key specifications as for clojure.spec/keys. All of the arguments
   are eval'ed, so they may be vars containing the definition(s). All
   of the arguments must evaluate to compile-time map constants."
  [& map-specs]
  (let [map-spec (->> map-specs
                      (map eval)
                      (apply merge-with set/union))]
    `(s/merge (s/keys ~@(apply concat (vec map-spec)))
              (s/map-of ~(allowed-keys map-spec) any?))))

(defmacro constrained-map
  "Creates an open map spec using the supplied keys specs with the
   additional contraint that all unspecified entries must match the
   given key and value specs. The keys specs will be evaluated."
  [key-spec value-spec & map-specs]
  (let [map-spec (->> map-specs
                      (map eval)
                      (apply merge-with set/union))]
    `(s/merge
       (s/every (s/or :attrs (s/tuple ~(allowed-keys map-spec) any?)
                      :link (s/tuple ~key-spec ~value-spec)))
       (s/keys ~@(apply concat (vec map-spec))))))
