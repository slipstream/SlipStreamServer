(ns com.sixsq.slipstream.ssclj.resources.common.dynamic-load
  "Utilities for loading information from CIMI resources dynamically."
  (:require
    [clojure.tools.logging                                    :as log]
    [clojure.java.classpath                                   :as cp]
    [clojure.tools.namespace.find                             :as nsf]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils  :as du]))

(defn filter-namespaces
  "Returns symbols for all of the namespaces that match the given filter
   function."
  [f]
  (->> (cp/classpath)
       (nsf/find-namespaces)
       (filter f)))

(defn load-namespace
  "Dynamically loads the given namespace, returning the namespace.
   Will return nil if the namespace could not be loaded."
  [ns-sym]
  (try
    (require ns-sym)
    (log/info "loaded namespace:" ns-sym)
    (find-ns ns-sym)
    (catch Exception _
      (log/error "could not load namespace:" ns-sym)
      nil)))

(defn resource?
  "If the given symbol represents a resource namespace, the symbol
   is returned; false otherwise.  Resource namespaces have the prefix
   'com.sixsq.slipstream.ssclj.resources.' and do not contain the
   string 'test'."
  [sym]
  (let [ns-name (name sym)]
    (and
      (re-matches #"^com\.sixsq\.slipstream\.ssclj\.resources\.[\w-]+$" ns-name)
      (not (.contains ns-name "test"))
      sym)))

(defn resources
  "Returns symbols for all of the resource namespaces on the classpath."
  []
  (filter-namespaces resource?))

(defn get-ns-var
  "Retrieves the named var in the given namespace, returning
   nil if the var could not be found.  Function logs the success or
   failure of the request."
  [varname resource-ns]

  (let [v (ns-resolve resource-ns (symbol varname))]
    (if v
      (log/debug varname "found in" (ns-name resource-ns))
      (log/debug varname "NOT found in" (ns-name resource-ns)))
    v))

(defn get-resource-link
  "Returns a vector with the resource tag keyword and map with the
   :href keyword associated with the relative URL for the resource.
   Function returns nil if either value cannot be found for the
   resource."
  [resource-ns]
  (if-let [vtag (get-ns-var "resource-tag" resource-ns)]
    (if-let [vtype (get-ns-var "resource-url" resource-ns)]
      [(deref vtag) {:href (deref vtype)}])))

(defn- initialize-resource
  "Run a resource's initialization function if it exists."
  [resource-ns]
  (if-let [fvar (get-ns-var "initialize" resource-ns)]
    (try
      ((deref fvar))
      (log/info "initialized resource" (ns-name resource-ns))
      (catch Exception e
        (log/error "initializing" (ns-name resource-ns) "failed:" (.getMessage e))))))

(defn resource-routes
  "Returns a lazy sequence of all of the routes for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-namespace)
       (remove nil?)
       (map (partial get-ns-var "routes"))
       (remove nil?)
       (map deref)))

(defn get-resource-links
  "Returns a lazy sequence of all of the resource links for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-namespace)
       (remove nil?)
       (map get-resource-link)
       (remove nil?)))

(defn initialize
  "Runs the initialize function for all resources that define it."
  []
  (doall
    (->> (resources)
         (map load-namespace)
         (remove nil?)
         (map initialize-resource))))
