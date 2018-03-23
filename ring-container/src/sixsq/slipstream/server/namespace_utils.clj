(ns sixsq.slipstream.server.namespace-utils
  (:require [clojure.tools.logging :as log])
  (:gen-class))


(defn- log-and-throw
  "Logs a fatal error and then throws an exception with the given method and
   optional cause."
  ([msg]
   (log-and-throw msg nil))
  ([msg e]
   (log/fatal msg)
   (if e
     (throw (ex-info (str msg "\n" e) {}))
     (throw (ex-info msg {})))))

(defn as-symbol
  "Converts argument to symbol or throws an exception."
  [s]
  (try
    (symbol s)
    (catch Exception e
      (log-and-throw (str "invalid symbol: " s) e))))


(defn ns-and-var
  "Provides the namespace and name of the symbol as symbols. If either is nil,
   then an exception is thrown."
  [s]
  (let [result ((juxt namespace name) s)]
    (if (some nil? result)
      (log-and-throw (str "symbol (" s ") must be a complete, namespaced value"))
      (map symbol result))))


(defn resolve-var
  "Resolves the var in the given namespace or throws a descriptive exception."
  [ns f]
  (try
    (ns-resolve (find-ns ns) f)
    (catch Exception e
      (log-and-throw (str "could not resolve " f " in namespace " ns) e))))


(defn dyn-resolve
  "Dynamically requires the namespace of the given symbol and then resolves
   the name of the symbol in this namespace. Returns the var for the resolved
   symbol. Throws an exception if the symbol could not be resolved."
  [s]
  (let [[ns f] (ns-and-var (as-symbol s))]
    (try
      (require ns)
      (catch Exception e
        (log-and-throw (str "error requiring namespace: " ns))))

    (if-let [result (resolve-var ns f)]
      result
      (log-and-throw (str "symbol (" s ") was not found")))))