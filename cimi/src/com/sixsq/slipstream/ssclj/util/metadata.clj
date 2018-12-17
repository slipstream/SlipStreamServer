(ns com.sixsq.slipstream.ssclj.util.metadata
  (:require
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as resource-metadata]
    [spec-tools.json-schema :as jsc])
  (:import
    (clojure.lang Namespace)))


(defn strip-for-attributes
  [[attribute-name description]]
  (let [{:keys [name] :as desc} (select-keys description #{:name :namespace :uri :type
                                                           :providerMandatory :consumerMandatory :consumerWritable
                                                           :templateMutable :mutable
                                                           :displayName :description :help
                                                           :group :category :order :hidden :sensitive :lines})]
    (cond-> desc
            (nil? name) (assoc :name attribute-name))))


(defn extract-value-scope
  [[attribute-name {:keys [value-scope] :as description}]]
  (when (and attribute-name value-scope)
    [(keyword attribute-name) value-scope]))


(defn generate-attributes
  "generate the attributes and vscope fields of the resource metadata from the
   schema definition"
  [spec]
  (let [json (jsc/transform spec)

        attributes (->> json
                        :properties
                        (map strip-for-attributes)
                        (sort-by :name)
                        vec)

        vscope (->> json
                    :properties
                    (map extract-value-scope)
                    (remove nil?)
                    (into {}))]

    (cond-> {}
            (seq attributes) (assoc :attributes attributes)
            (seq vscope) (assoc :vscope vscope))))


(defn get-doc
  [resource-ns]
  (-> resource-ns meta :doc))


(defn get-type-uri
  [ns]
  (some-> ns
          (ns-resolve 'resource-url)
          deref))


(defn get-resource-name
  [ns]
  (or (some-> ns
              (ns-resolve 'resource-name)
              deref)
      (get-type-uri ns)))


(defn get-type-uri
  [ns]
  (some-> ns
          (ns-resolve 'resource-url)
          deref))


(defn get-actions
  [resource-ns]
  (some-> resource-ns
          (ns-resolve 'actions)
          deref))


(defn get-capabilities
  [resource-ns]
  (some-> resource-ns
          (ns-resolve 'capabilities)
          deref))


(defn get-spec-kw
  [resource-url]
  (when resource-url
    (keyword (str "com.sixsq.slipstream.ssclj.resources.spec." resource-url) resource-url)))


(defn get-spec
  [parent-ns resource-ns]
  (some-> (or parent-ns resource-ns)
          (ns-resolve 'resource-url)
          deref
          get-spec-kw))


(defn as-namespace
  [ns]
  (when ns
    (cond
      (instance? Namespace ns) ns
      (symbol? ns) (find-ns (symbol (namespace ns)))
      (keyword? ns) (find-ns (symbol (namespace ns)))
      (string? ns) (find-ns (symbol ns)))))


(defn generate-metadata
  "Generate the ResourceMetadata from the provided namespace"
  ([parent-ns spec]
   (generate-metadata nil parent-ns spec))
  ([child-ns parent-ns spec]
   (if-let [parent-ns (as-namespace parent-ns)]
     (let [child-ns (as-namespace child-ns)

           resource-name (cond-> (get-resource-name parent-ns)
                                 child-ns (str " -- " (get-resource-name child-ns)))

           doc (get-doc (or child-ns parent-ns))
           type-uri (cond-> (get-type-uri parent-ns)
                            child-ns (str "-" (get-type-uri child-ns)))

           common {:id          "resource-metadata/dummy-id"
                   :created     "1964-08-25T10:00:00.0Z"
                   :updated     "1964-08-25T10:00:00.0Z"
                   :resourceURI resource-metadata/resource-uri
                   :acl         {:owner {:principal "ADMIN", :type "ROLE"}
                                 :rules [{:principal "ANON", :type "ROLE", :right "VIEW"}]}
                   :typeURI     type-uri
                   :name        resource-name
                   :description doc}

           attributes (generate-attributes spec)

           actions (get-actions parent-ns)

           capabilities (get-capabilities parent-ns)]

       (if (and doc spec type-uri)
         (cond-> common
                 attributes (merge attributes)
                 (seq actions) (assoc :actions actions)
                 (seq capabilities) (assoc :capabilities capabilities))
         (log/error "namespace documentation, spec, and resource-uri cannot be null for" (str parent-ns))))
     (log/error "cannot find namespace for" (str parent-ns)))))
