(ns com.sixsq.slipstream.ssclj.resources.deployment.test-utils
  (:require [clojure.test :refer [deftest is]]
            [com.sixsq.slipstream.ssclj.resources.deployment.utils :as utils]))

(def image-a {:description "descr image-a"
              :path        "root/image-a"
              :name        "image-a"
              :type        "IMAGE"
              :logo        {:href "logo/href"}
              :content     {:loginUser   "root"
                            :created     "2018-07-25T10:07:43.309Z"
                            :updated     "2018-07-25T10:07:43.309Z"
                            :imageIDs    {:cloud-a 1
                                          :cloud-b 2
                                          :cloud-c 3}
                            :author      "test"
                            :networkType "public"
                            :commit      "commit image a"
                            :os          "Ubuntu"}
              :id          "module/image-a",
              :resourceURI "http://sixsq.com/slipstream/1/Module"})

(def image-b {:description "descr image-b"
              :path        "root/image-b"
              :name        "image-b"
              :type        "IMAGE"
              :logo        {:href "logo/href"}
              :content     {:parentModule image-a
                            :loginUser    "ubuntu"
                            :created      "2018-07-25T10:07:43.309Z"
                            :updated      "2018-07-25T10:07:43.309Z"
                            :imageIDs     {:cloud-a 10
                                           :cloud-b 2
                                           :cloud-d 4}
                            :author       "test"
                            :networkType  "public"
                            :commit       "commit image b"
                            :os           "Ubuntu"}
              :id          "module/image-b",
              :resourceURI "http://sixsq.com/slipstream/1/Module"})

(def comp-a {:description "Apache web server appliance with custom landing page.",
             :path        "examples/tutorials/service-testing/apache",
             ;:logo        {:href "logolink"},
             :content     {:parentModule     image-a
                           :updated          "2018-10-03T13:19:47.310Z",
                           :outputParameters [{:parameter "hostname", :description "hostname/ip of the image"}
                                              {:parameter "port", :description "Port", :value "8080"}
                                              {:parameter "instanceid", :description "Cloud instance id"}
                                              {:parameter "ready", :description "Server ready to recieve connections"}],
                           :created          "2018-10-03T13:19:47.310Z",
                           :targets          {:packages    ["yum-utils" "apache"]
                                              :postinstall "postinstall comp-a",
                                              :onVmRemove  "onVmRemove comp-a",
                                              :onVmAdd     "onVmAdd comp-a",
                                              :deployment  "deployment comp-a"
                                              },
                           :author           "super",
                           :networkType      "public",
                           :commit           "update ifb-core-cloud flavor"},
             :updated     "2018-10-03T13:19:47.347Z",
             :name        "apache",
             :type        "COMPONENT",
             :created     "2018-07-25T10:08:49.035Z",
             :id          "module/comp-a",
             :parentPath  "examples/tutorials/service-testing",
             :resourceURI "http://sixsq.com/slipstream/1/Module"})

(def comp-b {:description "Apache web server ++",
             :path        "examples/tutorials/service-testing/apache++",
             :content     {:parentModule     comp-a
                           :updated          "2018-10-03T13:19:47.310Z",
                           :outputParameters [{:parameter "hostname", :description "hostname/ip of the image"}
                                              {:parameter "port", :value "80"}
                                              {:parameter "instanceid", :description "Cloud instance id"}
                                              {:parameter "ready", :description "Server ready to recieve connections"}],
                           :created          "2018-10-03T13:19:47.310Z",
                           :targets          {:postinstall "postinstall comp-b",
                                              :onVmRemove  "onVmRemove comp-b",
                                              :packages    ["emacs"]
                                              :onVmAdd     "onVmAdd comp-b",
                                              :deployment  "deployment comp-b"
                                              },
                           :author           "super",
                           :networkType      "public",
                           :commit           "update ifb-core-cloud flavor"},
             :updated     "2018-10-03T13:19:47.347Z",
             :name        "apache++",
             :type        "COMPONENT",
             :created     "2018-07-25T10:08:49.035Z",
             :id          "module/comp-b",
             :parentPath  "examples/tutorials/service-testing",
             :resourceURI "http://sixsq.com/slipstream/1/Module"})

(def comp-c {:description "Apache web server +++",
             :path        "examples/tutorials/service-testing/apache+++",
             :content     {:parentModule     (assoc-in comp-b [:content :parentModule :content :parentModule] image-b)
                           :updated          "2018-10-03T13:19:47.310Z",
                           :outputParameters [{:parameter "hostname", :description "hostname/ip of the image"}
                                              {:parameter "port", :value "80"}
                                              {:parameter "instanceid", :description "Cloud instance id"}
                                              {:parameter "ready", :description "Server ready to recieve connections"}],
                           :created          "2018-10-03T13:19:47.310Z",
                           :targets          {:deployment "deployment comp-c"},
                           :author           "super",
                           :networkType      "public",
                           :commit           "update ifb-core-cloud flavor"},
             :updated     "2018-10-03T13:19:47.347Z",
             :name        "apache++",
             :type        "COMPONENT",
             :created     "2018-07-25T10:08:49.035Z",
             :id          "module/comp-b",
             :parentPath  "examples/tutorials/service-testing",
             :resourceURI "http://sixsq.com/slipstream/1/Module"})

(def app-a {:description "Deployment",
            :path        "examples/tutorials/service-testing/system",
            :content     {:updated "2018-07-25T10:09:18.955Z",
                          :created "2018-07-25T10:09:18.955Z",
                          :author  "super",
                          :nodes   [{:node         "comp-c",
                                     :multiplicity 1,
                                     :component    comp-c}
                                    {:node              "image-b", :multiplicity 1,
                                     :component         image-b,
                                     :parameterMappings [{:parameter "image-b.port",
                                                          :value     "comp-c:port", :mapped true}
                                                         {:parameter "image-b.ready",
                                                          :value     "comp-c:ready", :mapped true}
                                                         {:parameter "image-b.hostname",
                                                          :value     "comp-c:hostname", :mapped true}]}]
                          :commit  "update image ids"},
            :updated     "2018-07-25T10:09:18.972Z",
            :name        "system",
            :type        "APPLICATION",
            :created     "2018-07-25T10:09:18.583Z",
            :id          "module/app",
            :parentPath  "examples/tutorials/service-testing",
            :resourceURI "http://sixsq.com/slipstream/1/Module"})


(deftest test-create-template-from-simple-image
  (is
    (=
      (utils/create-deployment-template {:module image-a})
      {:module
       {:content
                     {:author           "test"
                      :commit           "commit image a"
                      :created          "2018-07-25T10:07:43.309Z"
                      :imageIDs         {:cloud-a 1
                                         :cloud-b 2
                                         :cloud-c 3}
                      :inputParameters
                                        '({:description "Cloud credential ID for managing node deployment"
                                           :parameter   "credential.id"}
                                           {:description "Publish ports [<PROTOCOL>:<PUBLISHED_PORT>:<TARGET_PORT> ...] (e.g. 'tcp:20000:22 udp::69')"
                                            :parameter   "cloud.node.publish.ports"})
                      :loginUser        "root"
                      :networkType      "public"
                      :os               "Ubuntu"
                      :outputParameters '({:description "SSH keypair name used"
                                           :parameter   "keypair.name"}
                                           {:description "Cloud instance id"
                                            :parameter   "instanceid"}
                                           {:description "Custom state"
                                            :parameter   "statecustom"}
                                           {:description "Hostname or IP address of the image"
                                            :parameter   "hostname"}
                                           {:description "Machine abort flag, set when aborting"
                                            :parameter   "abort"}
                                           {:description "SSH password if available"
                                            :parameter   "password.ssh"}
                                           {:description "SSH URL to connect to virtual machine"
                                            :parameter   "url.ssh"}
                                           {:description "'true' when current state is completed"
                                            :parameter   "complete"}
                                           {:description "Optional service URL for virtual machine"
                                            :parameter   "url.service"})
                      :targets          {}
                      :updated          "2018-07-25T10:07:43.309Z"}
        :description "descr image-a"
        :logo        {:href "logo/href"}
        :name        "image-a"
        :path        "root/image-a"
        :id          "module/image-a"
        :resourceURI "http://sixsq.com/slipstream/1/Module"
        :type        "IMAGE"}})))

(deftest test-create-template-from-image-with-parent
  (is (= (utils/create-deployment-template {:module image-b})
         {:module
          {:content     {:author           "test"
                         :commit           "commit image b"
                         :created          "2018-07-25T10:07:43.309Z"
                         :imageIDs         {:cloud-a 10
                                            :cloud-b 2
                                            :cloud-c 3
                                            :cloud-d 4}
                         :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                              :parameter   "credential.id"}
                                              {:description "Publish ports [<PROTOCOL>:<PUBLISHED_PORT>:<TARGET_PORT> ...] (e.g. 'tcp:20000:22 udp::69')"
                                               :parameter   "cloud.node.publish.ports"})
                         :loginUser        "ubuntu"
                         :networkType      "public"
                         :os               "Ubuntu"
                         :outputParameters '({:description "SSH keypair name used"
                                              :parameter   "keypair.name"}
                                              {:description "Cloud instance id"
                                               :parameter   "instanceid"}
                                              {:description "Custom state"
                                               :parameter   "statecustom"}
                                              {:description "Hostname or IP address of the image"
                                               :parameter   "hostname"}
                                              {:description "Machine abort flag, set when aborting"
                                               :parameter   "abort"}
                                              {:description "SSH password if available"
                                               :parameter   "password.ssh"}
                                              {:description "SSH URL to connect to virtual machine"
                                               :parameter   "url.ssh"}
                                              {:description "'true' when current state is completed"
                                               :parameter   "complete"}
                                              {:description "Optional service URL for virtual machine"
                                               :parameter   "url.service"})
                         :targets          {}
                         :updated          "2018-07-25T10:07:43.309Z"}
           :description "descr image-b"
           :logo        {:href "logo/href"}
           :name        "image-b"
           :path        "root/image-b"
           :resourceURI "http://sixsq.com/slipstream/1/Module"
           :id          "module/image-b"
           :type        "IMAGE"}})))

(deftest test-create-template-from-component
  (is (= (utils/create-deployment-template {:module comp-a})
         {:module
          {:content     {:author           "super"
                         :commit           "update ifb-core-cloud flavor"
                         :created          "2018-10-03T13:19:47.310Z"
                         :imageIDs         {:cloud-a 1
                                            :cloud-b 2
                                            :cloud-c 3}
                         :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                              :parameter   "credential.id"}
                                              {:description "Publish ports [<PROTOCOL>:<PUBLISHED_PORT>:<TARGET_PORT> ...] (e.g. 'tcp:20000:22 udp::69')"
                                               :parameter   "cloud.node.publish.ports"})
                         :loginUser        "root"
                         :networkType      "public"
                         :os               "Ubuntu"
                         :outputParameters '({:description "Server ready to recieve connections"
                                              :parameter   "ready"}
                                              {:description "SSH keypair name used"
                                               :parameter   "keypair.name"}
                                              {:description "Cloud instance id"
                                               :parameter   "instanceid"}
                                              {:description "Custom state"
                                               :parameter   "statecustom"}
                                              {:description "hostname/ip of the image"
                                               :parameter   "hostname"}
                                              {:description "Port"
                                               :parameter   "port"
                                               :value       "8080"}
                                              {:description "Machine abort flag, set when aborting"
                                               :parameter   "abort"}
                                              {:description "SSH password if available"
                                               :parameter   "password.ssh"}
                                              {:description "SSH URL to connect to virtual machine"
                                               :parameter   "url.ssh"}
                                              {:description "'true' when current state is completed"
                                               :parameter   "complete"}
                                              {:description "Optional service URL for virtual machine"
                                               :parameter   "url.service"})
                         :targets          {:deployment  '("deployment comp-a")
                                            :onVmAdd     '("onVmAdd comp-a")
                                            :onVmRemove  '("onVmRemove comp-a")
                                            :packages    '("yum-utils"
                                                            "apache")
                                            :postinstall '("postinstall comp-a")}
                         :updated          "2018-10-03T13:19:47.310Z"}
           :created     "2018-07-25T10:08:49.035Z"
           :description "Apache web server appliance with custom landing page."
           :id          "module/comp-a"
           :name        "apache"
           :parentPath  "examples/tutorials/service-testing"
           :path        "examples/tutorials/service-testing/apache"
           :resourceURI "http://sixsq.com/slipstream/1/Module"
           :type        "COMPONENT"
           :updated     "2018-10-03T13:19:47.347Z"}})))

(deftest test-create-template-from-component-with-heritage
  (is (= (utils/create-deployment-template {:module comp-b})
         {:module
          {:content     {:author           "super"
                         :commit           "update ifb-core-cloud flavor"
                         :created          "2018-10-03T13:19:47.310Z"
                         :imageIDs         {:cloud-a 1
                                            :cloud-b 2
                                            :cloud-c 3}
                         :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                              :parameter   "credential.id"}
                                              {:description "Publish ports [<PROTOCOL>:<PUBLISHED_PORT>:<TARGET_PORT> ...] (e.g. 'tcp:20000:22 udp::69')"
                                               :parameter   "cloud.node.publish.ports"})
                         :loginUser        "root"
                         :networkType      "public"
                         :os               "Ubuntu"
                         :outputParameters '({:description "Server ready to recieve connections"
                                              :parameter   "ready"}
                                              {:description "SSH keypair name used"
                                               :parameter   "keypair.name"}
                                              {:description "Cloud instance id"
                                               :parameter   "instanceid"}
                                              {:description "Custom state"
                                               :parameter   "statecustom"}
                                              {:description "hostname/ip of the image"
                                               :parameter   "hostname"}
                                              {:description "Port"
                                               :parameter   "port"
                                               :value       "80"}
                                              {:description "Machine abort flag, set when aborting"
                                               :parameter   "abort"}
                                              {:description "SSH password if available"
                                               :parameter   "password.ssh"}
                                              {:description "SSH URL to connect to virtual machine"
                                               :parameter   "url.ssh"}
                                              {:description "'true' when current state is completed"
                                               :parameter   "complete"}
                                              {:description "Optional service URL for virtual machine"
                                               :parameter   "url.service"})
                         :targets          {:deployment  '("deployment comp-a"
                                                            "deployment comp-b")
                                            :onVmAdd     '("onVmAdd comp-a"
                                                            "onVmAdd comp-b")
                                            :onVmRemove  '("onVmRemove comp-a"
                                                            "onVmRemove comp-b")
                                            :packages    '("yum-utils"
                                                            "apache"
                                                            "emacs")
                                            :postinstall '("postinstall comp-a"
                                                            "postinstall comp-b")}
                         :updated          "2018-10-03T13:19:47.310Z"}
           :created     "2018-07-25T10:08:49.035Z"
           :description "Apache web server ++"
           :id          "module/comp-b"
           :name        "apache++"
           :parentPath  "examples/tutorials/service-testing"
           :path        "examples/tutorials/service-testing/apache++"
           :resourceURI "http://sixsq.com/slipstream/1/Module"
           :type        "COMPONENT"
           :updated     "2018-10-03T13:19:47.347Z"}})))

(deftest test-create-template-from-component-with-heritage-and-image-with-parent
  (is (= (utils/create-deployment-template {:module comp-c})
         {:module
          {:content     {:author          "super"
                         :commit          "update ifb-core-cloud flavor"
                         :created         "2018-10-03T13:19:47.310Z"
                         :imageIDs        {:cloud-a 10
                                           :cloud-b 2
                                           :cloud-c 3
                                           :cloud-d 4}
                         :inputParameters '({:description "Cloud credential ID for managing node deployment"
                                             :parameter   "credential.id"}
                                             {:description "Publish ports [<PROTOCOL>:<PUBLISHED_PORT>:<TARGET_PORT> ...] (e.g. 'tcp:20000:22 udp::69')"
                                              :parameter   "cloud.node.publish.ports"})
                         :loginUser       "ubuntu"
                         :networkType     "public"
                         :os              "Ubuntu"
                         :outputParameters
                                          '({:description "Server ready to recieve connections"
                                             :parameter   "ready"}
                                             {:description "SSH keypair name used"
                                              :parameter   "keypair.name"}
                                             {:description "Cloud instance id"
                                              :parameter   "instanceid"}
                                             {:description "Custom state"
                                              :parameter   "statecustom"}
                                             {:description "hostname/ip of the image"
                                              :parameter   "hostname"}
                                             {:description "Port"
                                              :parameter   "port"
                                              :value       "80"}
                                             {:description "Machine abort flag, set when aborting"
                                              :parameter   "abort"}
                                             {:description "SSH password if available"
                                              :parameter   "password.ssh"}
                                             {:description "SSH URL to connect to virtual machine"
                                              :parameter   "url.ssh"}
                                             {:description "'true' when current state is completed"
                                              :parameter   "complete"}
                                             {:description "Optional service URL for virtual machine"
                                              :parameter   "url.service"})
                         :targets         {:deployment  '("deployment comp-a"
                                                           "deployment comp-b"
                                                           "deployment comp-c")
                                           :onVmAdd     '("onVmAdd comp-a"
                                                           "onVmAdd comp-b")
                                           :onVmRemove  '("onVmRemove comp-a"
                                                           "onVmRemove comp-b")
                                           :packages    '("yum-utils"
                                                           "apache"
                                                           "emacs")
                                           :postinstall '("postinstall comp-a"
                                                           "postinstall comp-b")}
                         :updated         "2018-10-03T13:19:47.310Z"}
           :created     "2018-07-25T10:08:49.035Z"
           :description "Apache web server +++"
           :id          "module/comp-b"
           :name        "apache++"
           :parentPath  "examples/tutorials/service-testing"
           :path        "examples/tutorials/service-testing/apache+++"
           :resourceURI "http://sixsq.com/slipstream/1/Module"
           :type        "COMPONENT"
           :updated     "2018-10-03T13:19:47.347Z"}})))
