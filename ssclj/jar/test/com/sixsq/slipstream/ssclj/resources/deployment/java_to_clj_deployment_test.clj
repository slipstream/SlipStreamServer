(ns com.sixsq.slipstream.ssclj.resources.deployment.java-to-clj-deployment-test
  (:require [com.sixsq.slipstream.ssclj.resources.deployment.java-to-clj-deployment :refer :all]
            [clojure.test :refer :all]
            [clojure.data.json :as json]
            [clojure.data :as data]))


(def test-deployment-application-json
  (-> "test-resources/deployment-service-testing.json"
      slurp
      (json/read-str :key-fn keyword)))

(def test-deployment-build-json
  (-> "test-resources/deployment-build-apache-testing.json"
      slurp
      (json/read-str :key-fn keyword)))

(def test-deployment-image-json
  (-> "test-resources/deployment-image-wordpress-testing.json"
      slurp
      (json/read-str :key-fn keyword)))

(def deployment-application-result-edn
  {:id       "deployment/97089b96-5d99-4ccd-9bfe-99ba3ca21ae2",
   :state    "Done",
   :type     "Orchestration",
   :category "Deployment",
   :module-resource-uri
             "module/examples/tutorials/service-testing/system/1940",
   :mutable  false,
   :keep-running "always",
   :nodes
   {:apache
    {:runtime-parameters
     {:scale.iaas.done
                  {:description
                          "Orchestrator sets to 'true' after scaling the node instance",
                   :value "false"},
      :nuvlabox-carl-cori.custom.vm.template
                  {:description "Additional custom textual VM template", :value nil},
      :ready
                  {:description "Server ready to recieve connections",
                   :value       "true",
                   :mapped-to   ["testclient.1:webserver.ready"]},
      :nuvlabox-carl-cori.is.firewall
                  {:description "Firewalling", :value "false"},
      :vmstate
                  {:description "State of the VM, according to the cloud layer",
                   :value       "Unknown"},
      :pre.scale.done
                  {:description
                          "Node instance sets to 'true' after running pre-scale script",
                   :value "false"},
      :nuvlabox-carl-cori.cpu
                  {:description "Number of CPUs (i.e. virtual cores)", :value "1"},
      :disk.attached.device
                  {:description
                          "Attached device name after the VM's vertical scaling",
                   :value nil},
      :instanceid {:description "Cloud instance id", :value "282"},
      :extra.disk.volatile
                  {:description "Volatile extra disk in GB", :value nil},
      :statecustom
                  {:description "Custom state",
                   :value
                                "Executing script 'module/examples/tutorials/service-testing/apache/13791:Deployment'"},
      :ids
                  {:description "IDs of the machines in a mutable deployment.",
                   :value       "1"},
      :hostname
                  {:description "hostname/ip of the image",
                   :value       "172.16.0.16",
                   :mapped-to   ["testclient.1:webserver.hostname"]},
      :port
                  {:description "Port",
                   :value       "8080",
                   :mapped-to   ["testclient.1:webserver.port"]},
      :abort
                  {:description "Machine abort flag, set when aborting", :value nil},
      :nuvlabox-carl-cori.network.specific.name
                  {:description "Network name", :value nil},
      :image.id
                  {:description "Cloud image id",
                   :value       "d88f85a9489e5c05aa2f06a4a878826f"},
      :scale.state
                  {:description "Defined scalability state", :value "operational"},
      :disk.detach.device
                  {:description
                          "Name of the block device to detach from the VM during vertical scaling",
                   :value nil},
      :is.orchestrator
                  {:description "True if it's an orchestrator", :value "false"},
      :nuvlabox-carl-cori.ram
                  {:description "Amount of RAM, in GB", :value "0.5"},
      :id         {:description "Node instance id", :value "1"},
      :url.ssh
                  {:description "SSH URL to connect to virtual machine",
                   :value       "ssh://root@172.16.0.16"},
      :complete
                  {:description "'true' when current state is completed",
                   :value       "false"},
      :network    {:description "Network type", :value "Public"},
      :url.service
                  {:description "Optional service URL for virtual machine",
                   :value       "http://172.16.0.16:8080"},
      :nuvlabox-carl-cori.contextualization.type
                  {:description "Contextualization type", :value "one-context"},
      :nodename   {:description "Nodename", :value "apache"},
      :image.platform
                  {:description "Platform (eg: ubuntu, windows)", :value "ubuntu"},
      :disk.attach.size
                  {:description
                          "Size of the extra disk to attach to the VM during vertical scaling",
                   :value nil}},
     :parameters
     {:service-offer
                    {:description "",
                     :value       "service-offer/7327d738-f2f8-4925-9277-64dc72d6a283"},
      :cloudservice
                    {:description "Cloud Service where the node resides",
                     :value       "nuvlabox-carl-cori"},
      :cpu.nb       {:description "", :value ""},
      :multiplicity {:description "", :value "1"},
      :run-build-recipes
                    {:description
                            "Define if the SlipStream executor should run build recipes.",
                     :value "true"},
      :max-provisioning-failures
                    {:description "Max provisioning failures", :value "0"},
      :disk.GB      {:description "", :value ""},
      :node.increment
                    {:description "Current increment value for node instances ids",
                     :value       "2"},
      :ram.GB       {:description "", :value ""}}},
    :testclient
    {:runtime-parameters
     {:scale.iaas.done
                  {:description
                          "Orchestrator sets to 'true' after scaling the node instance",
                   :value "false"},
      :nuvlabox-carl-cori.custom.vm.template
                  {:description "Additional custom textual VM template", :value nil},
      :nuvlabox-carl-cori.is.firewall
                  {:description "Firewalling", :value "false"},
      :vmstate
                  {:description "State of the VM, according to the cloud layer",
                   :value       "Unknown"},
      :pre.scale.done
                  {:description
                          "Node instance sets to 'true' after running pre-scale script",
                   :value "false"},
      :nuvlabox-carl-cori.cpu
                  {:description "Number of CPUs (i.e. virtual cores)", :value "1"},
      :webserver.port
                  {:description "Port on which the web server listens",
                   :value       "8080"},
      :disk.attached.device
                  {:description
                          "Attached device name after the VM's vertical scaling",
                   :value nil},
      :instanceid {:description "Cloud instance id", :value "281"},
      :extra.disk.volatile
                  {:description "Volatile extra disk in GB", :value nil},
      :statecustom
                  {:description "Custom state",
                   :value       "OK: Hello from Apache deployed by SlipStream!"},
      :ids
                  {:description "IDs of the machines in a mutable deployment.",
                   :value       "1"},
      :hostname
                  {:description "hostname/ip of the image", :value "172.16.0.11"},
      :abort
                  {:description "Machine abort flag, set when aborting", :value nil},
      :nuvlabox-carl-cori.network.specific.name
                  {:description "Network name", :value nil},
      :image.id
                  {:description "Cloud image id",
                   :value       "d88f85a9489e5c05aa2f06a4a878826f"},
      :scale.state
                  {:description "Defined scalability state", :value "operational"},
      :disk.detach.device
                  {:description
                          "Name of the block device to detach from the VM during vertical scaling",
                   :value nil},
      :webserver.hostname
                  {:description "Server hostname", :value "172.16.0.16"},
      :is.orchestrator
                  {:description "True if it's an orchestrator", :value "false"},
      :nuvlabox-carl-cori.ram
                  {:description "Amount of RAM, in GB", :value "0.5"},
      :id         {:description "Node instance id", :value "1"},
      :url.ssh
                  {:description "SSH URL to connect to virtual machine",
                   :value       "ssh://root@172.16.0.11"},
      :complete
                  {:description "'true' when current state is completed",
                   :value       "false"},
      :webserver.ready
                  {:description "Server ready to recieve connections",
                   :value       "true"},
      :network    {:description "Network type", :value "Public"},
      :url.service
                  {:description "Optional service URL for virtual machine",
                   :value       "ssh://root@172.16.0.11"},
      :nuvlabox-carl-cori.contextualization.type
                  {:description "Contextualization type", :value "one-context"},
      :nodename   {:description "Nodename", :value "testclient"},
      :image.platform
                  {:description "Platform (eg: ubuntu, windows)", :value "ubuntu"},
      :disk.attach.size
                  {:description
                          "Size of the extra disk to attach to the VM during vertical scaling",
                   :value nil}},
     :parameters
     {:run-build-recipes
                    {:description
                            "Define if the SlipStream executor should run build recipes.",
                     :value "false"},
      :disk.GB      {:description "", :value ""},
      :ram.GB       {:description "", :value ""},
      :service-offer
                    {:description "",
                     :value       "service-offer/7327d738-f2f8-4925-9277-64dc72d6a283"},
      :cloudservice
                    {:description "Cloud Service where the node resides",
                     :value       "nuvlabox-carl-cori"},
      :cpu.nb       {:description "", :value ""},
      :max-provisioning-failures
                    {:description "Max provisioning failures", :value "0"},
      :node.increment
                    {:description "Current increment value for node instances ids",
                     :value       "2"},
      :multiplicity {:description "", :value "1"}}},
    :orchestrator-nuvlabox-carl-cori
    {:runtime-parameters
     {:vmstate
                  {:description "State of the VM, according to the cloud layer",
                   :value       "Unknown"},
      :instanceid {:description "Cloud instance id", :value "280"},
      :max.iaas.workers
                  {:description
                          "Max number of concurrently provisioned VMs by orchestrator",
                   :value "7"},
      :statecustom
                  {:description "Custom state",
                   :value       "No node instances to stop [2017-09-14T08:14:34Z]."},
      :hostname
                  {:description "hostname/ip of the image", :value "172.16.0.18"},
      :abort
                  {:description "Machine abort flag, set when aborting", :value nil},
      :is.orchestrator
                  {:description "True if it's an orchestrator", :value "true"},
      :url.ssh
                  {:description "SSH URL to connect to virtual machine",
                   :value       "ssh://root@172.16.0.18"},
      :complete
                  {:description "'true' when current state is completed",
                   :value       "false"},
      :url.service
                  {:description "Optional service URL for virtual machine",
                   :value       nil}}}}}
  )

(def deployment-build-image-result-edn
  {:id       "deployment/380798cb-bb04-41e4-8498-d8ee090e1643",
   :state    "Done",
   :type     "Machine",
   :category "Image",
   :module-resource-uri
             "module/examples/tutorials/service-testing/apache/5071",
   :mutable  false,
   :keep-running "always",
   :nodes
   {:machine
    {:runtime-parameters
     {:scale.iaas.done
                   {:description
                           "Orchestrator sets to 'true' after scaling the node instance",
                    :value "false"},
      :ready
                   {:description "Server ready to recieve connections", :value nil},
      :vmstate
                   {:description "State of the VM, according to the cloud layer",
                    :value       "Unknown"},
      :pre.scale.done
                   {:description
                           "Node instance sets to 'true' after running pre-scale script",
                    :value "false"},
      :disk.attached.device
                   {:description
                           "Attached device name after the VM's vertical scaling",
                    :value nil},
      :nuvlabox-stanley-cohen.is.firewall
                   {:description
                           "If this flag is set, the instance will be started on the internal network and on the natted network so it can act as a firewall for instances on the internal network.",
                    :value "false"},
      :instanceid  {:description "Cloud instance id", :value "120"},
      :extra.disk.volatile
                   {:description "Volatile extra disk in GB", :value nil},
      :statecustom {:description "Custom state", :value "Image saved !"},
      :hostname
                   {:description "hostname/ip of the image", :value "172.16.0.15"},
      :port        {:description "Port", :value "8080"},
      :abort
                   {:description "Machine abort flag, set when aborting", :value nil},
      :image.id
                   {:description "Cloud image id",
                    :value       "d88f85a9489e5c05aa2f06a4a878826f"},
      :scale.state
                   {:description "Defined scalability state", :value "creating"},
      :disk.detach.device
                   {:description
                           "Name of the block device to detach from the VM during vertical scaling",
                    :value nil},
      :is.orchestrator
                   {:description "True if it's an orchestrator", :value "false"},
      :nuvlabox-stanley-cohen.cpu
                   {:description "Number of CPUs (i.e. virtual cores)", :value "1"},
      :nuvlabox-stanley-cohen.custom.vm.template
                   {:description "Additional custom VM template", :value nil},
      :url.ssh
                   {:description "SSH URL to connect to virtual machine",
                    :value       "ssh://root@172.16.0.15"},
      :nuvlabox-stanley-cohen.ram
                   {:description "Amount of RAM, in GB", :value "0.5"},
      :complete
                   {:description "'true' when current state is completed",
                    :value       "false"},
      :network     {:description "Network type", :value "Public"},
      :url.service
                   {:description "Optional service URL for virtual machine",
                    :value       nil},
      :nuvlabox-stanley-cohen.network.specific.name
                   {:description "Network name", :value nil},
      :image.platform
                   {:description "Platform (eg: ubuntu, windows)", :value "ubuntu"},
      :disk.attach.size
                   {:description
                           "Size of the extra disk to attach to the VM during vertical scaling",
                    :value nil}},
     :parameters
     {:cloudservice {:description "", :value "nuvlabox-stanley-cohen"}}}}}
  )

(def deployment-image-result-edn
  {:id "deployment/0e0fca32-1bbb-40e3-b2cd-2d97a318694a",
   :state "Done",
   :type "Run",
   :category "Image",
   :module-resource-uri "module/apps/WordPress/wordpress/3842",
   :mutable false,
   :keep-running "always",
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
      :cloudservice {:description "", :value "nuvlabox-bertil-ohlin"}}}}}
  )

(deftest load-deployment-application-transform
  (let [result (transform test-deployment-application-json)
        diff-result (data/diff result deployment-application-result-edn)]
    (is (and (-> diff-result first nil?) (-> diff-result second nil?)))))


(deftest load-deployment-build-transform
  (let [result (transform test-deployment-build-json)
        diff-result (data/diff result deployment-build-image-result-edn)]
    (clojure.pprint/pprint result)
    (is (and (-> diff-result first nil?) (-> diff-result second nil?)))))

(deftest load-deployment-image-transform
  (let [result (transform test-deployment-image-json)
        diff-result (data/diff result deployment-image-result-edn)]
    (is (and (-> diff-result first nil?) (-> diff-result second nil?)))))


