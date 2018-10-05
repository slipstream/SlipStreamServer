(ns com.sixsq.slipstream.ssclj.resources.deployment.utils)


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


(defn add-service-params
  [{{:keys [type]} :module :as deployment-template}]
  (if (#{"COMPONENT" "IMAGE"} type)
    ;;TODO perhaps node params should be add at module creation to simplify mappings for applications in UI,
    ;;TODO potential problem is if we decide to add a remove node params at creation time on existing modules and depl templ...
    (-> deployment-template
        (update-in [:module :content :outputParameters] concat node-output-params)
        (update-in [:module :content :inputParameters] concat node-input-params)
        (assoc :outputParameters global-parameters))
    deployment-template                                     ;;TODO add service params for each node
    ))
