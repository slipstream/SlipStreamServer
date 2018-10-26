(ns com.sixsq.slipstream.ssclj.util.metadata
  (:require
    [spec-tools.json-schema :as jsc]))


(defn strip
  [[attribute-name description]]
  (let [{:keys [name] :as desc} (select-keys description #{:name :namespace :uri :type
                                                           :providerMandatory :consumerMandatory :mutable :consumerWritable
                                                           :displayName :description :help
                                                           :group :category :order :hidden :sensitive :lines})]
    (cond-> desc
            (nil? name) (assoc :name attribute-name))))


(defn generate
  "generate the ResourceMetadata from the given spec"
  [typeURI spec]
  (let [common {:id          "resource-metadata/dummy-id"
                :created     "1964-08-25T10:00:00.0Z"
                :updated     "1964-08-25T10:00:00.0Z"
                :resourceURI "https://example.org/ResourceMetadata"
                :acl         {:owner {:principal "ADMIN", :type "ROLE"}}}
        attributes (->> (jsc/transform spec)
                        :properties
                        (map strip)
                        (sort-by :name)
                        vec)]
    (merge common
           {:typeURI    typeURI
            :attributes attributes})))
