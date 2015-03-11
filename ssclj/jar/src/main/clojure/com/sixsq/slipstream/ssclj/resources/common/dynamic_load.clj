(ns com.sixsq.slipstream.ssclj.resources.common.dynamic-load
  "Utilities for loading information from CIMI resources dynamically."
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [ring.util.response :as r]
    [clojure.tools.logging :as log]
    [clojure.java.classpath :as cp]
    [clojure.tools.namespace.find :as nsf]))

(defn resource?
  "If the given symbol represents a resource namespace, the symbol
   is returned; nil otherwise.  Resource namespaces have the prefix
   'com.sixsq.slipstream.ssclj.resources.'. "
  [sym]
  (->> (name sym)
       (re-matches #"^com\.sixsq\.slipstream\.ssclj\.resources\.[\w-]+$")
       (first)))

(defn resources
  "Returns the namespaces of all resources available on the classpath."
  []
  (->> (cp/classpath)
       (nsf/find-namespaces)
       (filter resource?)))

(defn load-resource
  "Dynamically loads the given namespace, returning the namespace.
   Will return nil if the namespace could not be loaded."
  [resource-ns]
  (try
    (require resource-ns)
    (log/info "loaded resource namespace:" (name resource-ns))
    resource-ns
    (catch Exception e
      (log/warn "could not load resource namespace:" (name resource-ns)))))

(defn get-ns-var
  "Retrieves the named var in the given namespace, returning
   nil if the var could not be found.  Function logs the success or
   failure of the request."
  [varname resource-ns]  
  (if-let [value (-> resource-ns
                     (name)
                     (str "/" varname)
                     (symbol)
                     (find-var))]
    (do
      (log/info "retrieved" varname "for" (name resource-ns))
      value)
    (do
      (log/warn "did NOT retrieve" varname "for" (name resource-ns)))))

(defn get-resource-link
  "Returns a vector with the resource tag keyword and map with the
   :href keyword associated with the relative URL for the resource.
   Function returns nil if either value cannot be found for the
   resource."
  [resource-ns]
  (if-let [vtag (get-ns-var "resource-tag" resource-ns)]
    (if-let [vtype (get-ns-var "resource-name" resource-ns)]
      [(deref vtag) {:href (deref vtype)}])))

(defn resource-routes
  "Returns a lazy sequence of all of the routes for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-resource)
       (remove nil?)
       (map (partial get-ns-var "routes"))
       (remove nil?)
       (map deref)))

(defn get-resource-links
  "Returns a lazy sequence of all of the resource links for resources
   discovered on the classpath."
  []
  (->> (resources)
       (map load-resource)
       (remove nil?)
       (map get-resource-link)
       (remove nil?)))
