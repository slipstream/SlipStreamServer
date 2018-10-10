(ns com.sixsq.slipstream.ssclj.resources.deployment.utils
  (:require [taoensso.timbre :as log]))


(def node-output-params [{:parameter   "abort"
                          :description "Machine abort flag, set when aborting"},
                         {:parameter   "url.ssh"
                          :description "SSH URL to connect to virtual machine"},
                         {:parameter   "password.ssh"
                          :description "SSH password if available"},
                         {:parameter   "keypair.name"
                          :description "SSH keypair name used"},
                         {:parameter   "url.service"
                          :description "Optional service URL for virtual machine"},
                         {:parameter   "statecustom"
                          :description "Custom state"},
                         {:parameter   "complete"
                          :description "'true' when current state is completed"},
                         {:parameter   "instanceid"
                          :description "Cloud instance id"},
                         {:parameter   "hostname"
                          :description "Hostname or IP address of the image"}])

(def node-input-params [{:parameter   "credential.id"
                         :description "Cloud credential ID for managing node deployment"}
                        {:parameter   "cloud.node.publish.ports"
                         :description (str "Publish ports [<PROTOCOL>:<PUBLISHED_PORT>:<TARGET_PORT> ...] "
                                           "(e.g. 'tcp:20000:22 udp::69')")}])

(def global-parameters [{:parameter   "ss:complete"
                         :description "Global complete flag, set when run completed"},
                        {:parameter   "ss:state"
                         :description "Global execution state"
                         :value       "Executing"},
                        {:parameter   "ss:abort"
                         :description "Run abort flag, set when aborting"},
                        {:parameter   "ss:url.service"
                         :description "Optional service URL for the deployment"}])


(defn add-global-params
  [deployment-template]
  (assoc deployment-template :outputParameters global-parameters))


(defn add-service-params
  [module-comp-or-img]
  (-> module-comp-or-img
      (update-in [:content :outputParameters] concat node-output-params)
      (update-in [:content :inputParameters] concat node-input-params)))


(defn content-merge
  [{:keys [content] :as module} content-aggregator]
  (-> content
      (dissoc :parent)
      (merge content-aggregator)))


(defn params-reducer-fn
  [type]
  (fn [params-aggregator parameter]
    (let [param-name-key (-> parameter :parameter keyword)
          typed-parameter (assoc parameter :type type)]
      (if (contains? params-aggregator param-name-key)
        (update params-aggregator param-name-key
                merge typed-parameter (param-name-key params-aggregator))
        (assoc params-aggregator param-name-key typed-parameter)))))


(defn params-merge
  [{:keys [content] :as module} params-aggregator]
  (let [input-params (get content :inputParameters [])
        output-params (get content :outputParameters [])
        reduce-fn (fn [params-aggregator type params]
                    (reduce (params-reducer-fn type) params-aggregator params))]
    (-> params-aggregator
        (reduce-fn "input" input-params)
        (reduce-fn "output" output-params))))


(defn params->original-format
  [type params]
  (sequence
    (comp
      (filter #(= type (:type %)))
      (map #(dissoc % :type)))
    (vals params)))


(defn targets-merge
  [{:keys [content] :as module} targets-aggregator]
  (let [targets (get content :targets {})]
    (reduce (fn [targets-aggregator [k v]]
              (let [f (if (string? v) cons concat)]
                (assoc targets-aggregator k (f v (k targets-aggregator)))))
            targets-aggregator targets)))


(defn image-ids-merge
  [{:keys [content] :as module} image-ids-aggregator]
  (merge (get content :imageIDs {}) image-ids-aggregator))


(defn combine-modules-comp-img
  [module-comp-or-img]
  (let [module-meta (dissoc module-comp-or-img :content)]
    (loop [module module-comp-or-img
           content-aggregator {}
           params-aggregator {}
           targets-aggregator {}
           image-ids-aggregator {}]
      (if-not module
        (assoc module-meta :content (-> content-aggregator
                                        (assoc :inputParameters (params->original-format "input" params-aggregator))
                                        (assoc :outputParameters (params->original-format "output" params-aggregator))
                                        (assoc :targets targets-aggregator)
                                        (assoc :imageIDs image-ids-aggregator)))
        (recur (-> module :content :parent)
               (content-merge module content-aggregator)
               (params-merge module params-aggregator)
               (targets-merge module targets-aggregator)
               (image-ids-merge module image-ids-aggregator))))))


(defn create-deployment-template
  [{{:keys [type]} :module :as resolved-body}]
  (if (#{"COMPONENT" "IMAGE"} type)
    (let [combined-modules (-> resolved-body
                               :module
                               add-service-params
                               combine-modules-comp-img)]
      (assoc resolved-body :module combined-modules))
    (let [nodes (get-in resolved-body [:module :content :nodes])
          nodes-combined-modules (map (fn [{:keys [component] :as node}]
                                        (let [combined-module (-> component
                                                                  add-service-params
                                                                  combine-modules-comp-img)]
                                          (assoc node :component combined-module))) nodes)]
      (assoc-in resolved-body [:module :content :nodes] nodes-combined-modules))))