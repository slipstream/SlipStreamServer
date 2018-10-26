(ns com.sixsq.slipstream.ssclj.util.metadata
  (:require
    [spec-tools.json-schema :as jsc]))


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


(defn generate
  "generate the ResourceMetadata from the given spec"
  [typeURI spec]
  (let [common {:id          "resource-metadata/dummy-id"
                :created     "1964-08-25T10:00:00.0Z"
                :updated     "1964-08-25T10:00:00.0Z"
                :resourceURI "https://example.org/ResourceMetadata"
                :acl         {:owner {:principal "ADMIN", :type "ROLE"}}
                :typeURI     typeURI}

        json (jsc/transform spec)

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

    (cond-> common
            (seq attributes) (assoc :attributes attributes)
            (seq vscope) (assoc :vscope vscope))))
