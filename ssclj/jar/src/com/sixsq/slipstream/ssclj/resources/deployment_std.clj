(ns com.sixsq.slipstream.ssclj.resources.deployment-std
  (:require
    [com.sixsq.slipstream.ssclj.resources.spec.deployment]
    [com.sixsq.slipstream.ssclj.resources.spec.deployment-template-std]
    [com.sixsq.slipstream.ssclj.resources.deployment :as d]
    [com.sixsq.slipstream.ssclj.resources.deployment-template-std :as dtpl]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u]))

;;
;; validate the create resource
;;
(def create-validate-fn (u/create-spec-validation-fn :cimi/deployment-template.std-create))
(defmethod d/create-validate-subtype dtpl/method
  [resource]
  (create-validate-fn resource))

(def deplo-body  {:id "deployment/0e0fca32-1bbb-40e3-b2cd-2d97a318694a",
                  :state "Done",
                  :type "Run",
                  :category "Image",
                  :module-resource-uri "module/apps/WordPress/wordpress/3842",
                  :mutable false,
                  :nodes
                  {:machine
                   {:runtime-parameters
                    {:admin_password
                                 {:description "admin password", :value "8rsBZBM659jK"},
                     :wordpress_title
                                 {:description "Title (name) to give to the WordPress instance",
                                  :value "Change Me Please 2"},
                     :scale.iaas.done
                                 {:description
                                         "Orchestrator sets to 'true' after scaling the node instance",
                                  :value "false"},
                     :vmstate
                                 {:description "State of the VM, according to the cloud layer",
                                  :value "Unknown"},
                     :pre.scale.done
                                 {:description
                                         "Node instance sets to 'true' after running pre-scale script",
                                  :value "false"},
                     :disk.attached.device
                                 {:description
                                         "Attached device name after the VM's vertical scaling",
                                  :value nil},
                     :instanceid {:description "Cloud instance id", :value "23"},
                     :extra.disk.volatile
                                 {:description "Volatile extra disk in GB", :value nil},
                     :statecustom
                                 {:description "Custom state", :value "WordPress ready to go!"},
                     :hostname
                                 {:description "hostname/ip of the image", :value "172.16.0.13"},
                     :abort
                                 {:description "Machine abort flag, set when aborting", :value nil},
                     :nuvlabox-bertil-ohlin.ram
                                 {:description "Amount of RAM, in GB", :value "0.5"},
                     :image.id {:description "Cloud image id", :value "8"},
                     :scale.state
                                 {:description "Defined scalability state", :value "operational"},
                     :disk.detach.device
                                 {:description
                                         "Name of the block device to detach from the VM during vertical scaling",
                                  :value nil},
                     :nuvlabox-bertil-ohlin.cpu
                                 {:description "Number of CPUs (i.e. virtual cores)", :value "1"},
                     :is.orchestrator
                                 {:description "True if it's an orchestrator", :value "false"},
                     :nuvlabox-bertil-ohlin.network.specific.name
                                 {:description "Network name", :value nil},
                     :admin_email
                                 {:description "admin email", :value "admin@example.com"},
                     :mysql_password
                                 {:description "MySQL password", :value "AX9ytTh2vWF4"},
                     :url.ssh
                                 {:description "SSH URL to connect to virtual machine",
                                  :value "ssh://root@172.16.0.13"},
                     :complete
                                 {:description "'true' when current state is completed",
                                  :value "false"},
                     :network {:description "Network type", :value "Public"},
                     :url.service
                                 {:description "Optional service URL for virtual machine",
                                  :value "http://172.16.0.13:8080"},
                     :image.platform
                                 {:description "Platform (eg: ubuntu, windows)", :value "ubuntu"},
                     :nuvlabox-bertil-ohlin.custom.vm.template
                                 {:description "Additional custom VM template", :value nil},
                     :disk.attach.size
                                 {:description
                                         "Size of the extra disk to attach to the VM during vertical scaling",
                                  :value nil},
                     :nuvlabox-bertil-ohlin.is.firewall
                                 {:description
                                         "If this flag is set, the instance will be started on the internal network and on the natted network so it can act as a firewall for instances on the internal network.",
                                  :value "false"}},
                    :parameters
                    {:run-build-recipes
                                   {:description
                                           "Define if the SlipStream executor should run build recipes.",
                                    :value "false"},
                     :cloudservice {:description "", :value "nuvlabox-bertil-ohlin"}}}}})

;;
;; transform template into deployment resource
;; just strips method attribute and updates the resource URI
;;
(defmethod d/tpl->deployment dtpl/method
  [resource request]
  (-> deplo-body
      (dissoc :method)
      (assoc :resourceURI d/resource-uri))
  )

