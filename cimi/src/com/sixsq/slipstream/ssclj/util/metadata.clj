(ns com.sixsq.slipstream.ssclj.util.metadata
  (:require
    [spec-tools.json-schema :as jsc]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.ssclj.resources.resource-metadata :as resource-metadata])
  (:import
    (clojure.lang Namespace)))


(defn strip-for-attributes
  [[attribute-name description]]
  (let [{:keys [name] :as desc} (select-keys description #{:name :namespace :uri :type
                                                           :providerMandatory :consumerMandatory :mutable :consumerWritable
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


(defn get-resource-name
  [resource-ns]
  (some-> resource-ns
          (ns-resolve 'resource-name)
          deref))


(defn get-type-uri
  [resource-ns]
  (some-> resource-ns
          (ns-resolve 'resource-uri)
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
  [resource-ns]
  (some-> resource-ns
          (ns-resolve 'resource-url)
          deref
          get-spec-kw))


(defn generate-from-ns
  "generate the ResourceMetadata from the current (or provided) namespace"
  ([]
   (generate-from-ns *ns*))
  ([resource-ns]
   (if-let [resource-ns (if (instance? Namespace resource-ns)
                          resource-ns
                          (find-ns (symbol resource-ns)))]
     (let [resource-name (get-resource-name resource-ns)
           doc (get-doc resource-ns)
           spec (get-spec resource-ns)
           type-uri (get-type-uri resource-ns)

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

           actions (get-actions resource-ns)

           capabilities (get-capabilities resource-ns)]

       (if (and doc spec type-uri)
         (cond-> common
                 attributes (merge attributes)
                 (seq actions) (assoc :actions actions)
                 (seq capabilities) (assoc :capabilities capabilities))
         (log/error "namespace documentation, spec, and resource-uri cannot be null")))
     (log/error "cannot find namespace for " (str resource-ns)))))
