(ns com.sixsq.slipstream.ssclj.resources.deployment.java-to-clj-deployment
  (:require [clojure.string :as str]))



(defn is-category-image? [category]
  (= category "Image"))

(defn get-nodes-name [{{nodes-entry :entry} :nodes category :category :as module}]
  (if (is-category-image? category) ["machine"]
                                    (map (fn [{{name :name :as node} :node}] name) nodes-entry)
                                    ))

(defn extract-keep-running-value [{parameters :entry}]
  (let [parameter-keep-running (filter #(= (:string %) "General.keep-running") parameters)
        {{value :value} :parameter} (first parameter-keep-running)]
    (when (some? value) value)))

(defn extract-runtime-parameters-for-node [node-name {runtime-parameters :entry}]
  (let [runtime-parameters-for-node (filter #(or (str/starts-with? (:string %) (str node-name ":"))
                                                 (str/starts-with? (:string %) (str node-name ".1:")))
                                            runtime-parameters)
        runtime-parameters-for-node (filter #(not (contains? #{"cloudservice" "max-provisioning-failures"
                                                               "service-offer" "cpu.nb" "disk.GB" "ram.GB"
                                                               "multiplicity"}
                                                             (get-in % [:runtimeParameter :name])))
                                            runtime-parameters-for-node)
        runtime-parameters-for-node-useful-data
        (map
          (fn [{{name :name description :description value
                      :content mapped-to :mappedRuntimeParameterNames}
                :runtimeParameter :as runtime-parameter}]
            (let [runtime-parameter (if (str/blank? mapped-to)
                                      {:description description
                                       :value       value}
                                      {:description description
                                       :value       value
                                       :mapped-to   (str/split mapped-to #",")})]
              (list (keyword name) runtime-parameter))) runtime-parameters-for-node)
        runtime-parameters-map (->> runtime-parameters-for-node-useful-data
                                    (apply concat)
                                    (apply assoc {}))]
    {:runtime-parameters runtime-parameters-map}))

(defn extract-parameters-for-node [node-name {parameters :entry}]
  (let [parameters-for-node (filter #(str/starts-with? (:string %) (str node-name ":"))
                                    parameters)
        parameters-for-node (filter #(contains? #{"cloudservice" "max-provisioning-failures"
                                                  "service-offer" "cpu.nb" "disk.GB" "ram.GB"
                                                  "multiplicity" "run-build-recipes" "node.increment"}
                                                (-> (get-in % [:parameter :name])
                                                    (str/replace (re-pattern (str "^" node-name ":")) "")))
                                    parameters-for-node)
        parameters-for-node-useful-data
        (map
          (fn [{{name :name description :description value :value}
                :parameter :as parameter}]
            (let [parameter {:description description
                             :value       value}
                  name (str/replace name (re-pattern (str "^" node-name ":")) "")]
              (list (keyword name) parameter))) parameters-for-node)
        parameters-map (->> parameters-for-node-useful-data
                            (apply concat)
                            (apply assoc {}))]
    {:parameters parameters-map}))

(defn transform [{{uuid                :uuid
                   state               :state
                   type                :type
                   category            :category
                   module-resource-uri :moduleResourceUri
                   mutable             :mutable
                   cloudservices       :cloudservices
                   module              :module
                   cloud-service-names :cloudServiceNames
                   runtime-parameters  :runtimeParameters
                   parameters          :parameters
                   } :run :as java-run}]

  (let [deployment {:id                  (str "deployment/" uuid)
                    :state               state
                    :type                type
                    :category            category
                    :module-resource-uri module-resource-uri
                    :mutable             mutable
                    :keep-running        (extract-keep-running-value parameters)}
        nodes-name (get-nodes-name module)
        orchestrators-name (map #(str "orchestrator-" %) (str/split cloud-service-names #","))
        orchestrators-info (if (is-category-image? category)
                             nil
                             (->> orchestrators-name
                                  (map #(list (keyword %) (extract-runtime-parameters-for-node
                                                            % runtime-parameters)))
                                  (apply concat)
                                  (apply assoc {})))
        nodes-info (->> nodes-name
                        (map #(list (keyword %) (conj (extract-runtime-parameters-for-node
                                                        % runtime-parameters)
                                                      (extract-parameters-for-node
                                                        % parameters)
                                                      )))
                        (apply concat)
                        (apply assoc {}))
        nodes-info (conj nodes-info orchestrators-info)]

    (assoc deployment :nodes nodes-info)))