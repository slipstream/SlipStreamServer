(ns com.sixsq.slipstream.ssclj.resources.deployment.test-utils
  (:require [com.sixsq.slipstream.ssclj.resources.deployment.utils :as utils]
            [clojure.test :refer [deftest is]]
            [clojure.data.json :as json]
            [taoensso.timbre :as log]
            [clojure.pprint :as pprint]))

(def test-module-application-service-testing
  (json/read-str (slurp "test-resources/module.json") :key-fn keyword))

(def test-module-component-apache (-> test-module-application-service-testing
                                      :content :nodes first :component))

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
              :content     {:parent      image-a
                            :loginUser   "ubuntu"
                            :created     "2018-07-25T10:07:43.309Z"
                            :updated     "2018-07-25T10:07:43.309Z"
                            :imageIDs    {:cloud-a 10
                                          :cloud-b 2
                                          :cloud-d 4}
                            :author      "test"
                            :networkType "public"
                            :commit      "commit image b"
                            :os          "Ubuntu"}
              :id          "module/image-b",
              :resourceURI "http://sixsq.com/slipstream/1/Module"})

(def comp-a {:description "Apache web server appliance with custom landing page.",
             :path        "examples/tutorials/service-testing/apache",
             ;:logo        {:href "logolink"},
             :content     {:parent           image-a
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
             :content     {:parent           comp-a
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
             :content     {:parent           (assoc-in comp-b [:content :parent :content :parent] image-b)
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

(def app {:description "Deployment",
          :path        "examples/tutorials/service-testing/system",
          :content     {:updated "2018-07-25T10:09:18.955Z",
                        :created "2018-07-25T10:09:18.955Z",
                        :author  "super",
                        :nodes   [{:node         "comp-c",
                                   :multiplicity 1,
                                   :component    comp-c}
                                  {:node              "image-b", :multiplicity 1,
                                   :component         image-b,
                                   :parameterMappings [{:parameter "image-b.port", :value "comp-c:port", :mapped true}
                                                       {:parameter "image-b.ready", :value "comp-c:ready", :mapped true}
                                                       {:parameter "image-b.hostname", :value "comp-c:hostname", :mapped true}]}]
                        :commit  "update image ids"},
          :updated     "2018-07-25T10:09:18.972Z",
          :name        "system",
          :type        "APPLICATION",
          :created     "2018-07-25T10:09:18.583Z",
          :id          "module/app",
          :parentPath  "examples/tutorials/service-testing",
          :resourceURI "http://sixsq.com/slipstream/1/Module"})


(deftest test-create-template-from-simple-image
  (is (= (utils/create-deployment-template {:module image-a})
         {:module {:content     {:author           "test"
                                 :commit           "commit image a"
                                 :created          "2018-07-25T10:07:43.309Z"
                                 :imageIDs         {:cloud-a 1
                                                    :cloud-b 2
                                                    :cloud-c 3}
                                 :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                                      :parameter   "credential.id"})
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
         {:module {:content     {:author           "test"
                                 :commit           "commit image b"
                                 :created          "2018-07-25T10:07:43.309Z"
                                 :imageIDs         {:cloud-a 10
                                                    :cloud-b 2
                                                    :cloud-c 3
                                                    :cloud-d 4}
                                 :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                                      :parameter   "credential.id"})
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
         {:module {:content     {:author           "super"
                                 :commit           "update ifb-core-cloud flavor"
                                 :created          "2018-10-03T13:19:47.310Z"
                                 :imageIDs         {:cloud-a 1
                                                    :cloud-b 2
                                                    :cloud-c 3}
                                 :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                                      :parameter   "credential.id"})
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
         {:module {:content     {:author           "super"
                                 :commit           "update ifb-core-cloud flavor"
                                 :created          "2018-10-03T13:19:47.310Z"
                                 :imageIDs         {:cloud-a 1
                                                    :cloud-b 2
                                                    :cloud-c 3}
                                 :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                                      :parameter   "credential.id"})
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
         {:module {:content     {:author           "super"
                                 :commit           "update ifb-core-cloud flavor"
                                 :created          "2018-10-03T13:19:47.310Z"
                                 :imageIDs         {:cloud-a 10
                                                    :cloud-b 2
                                                    :cloud-c 3
                                                    :cloud-d 4}
                                 :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                                      :parameter   "credential.id"})
                                 :loginUser        "ubuntu"
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
                                 :updated          "2018-10-03T13:19:47.310Z"}
                   :created     "2018-07-25T10:08:49.035Z"
                   :description "Apache web server +++"
                   :id          "module/comp-b"
                   :name        "apache++"
                   :parentPath  "examples/tutorials/service-testing"
                   :path        "examples/tutorials/service-testing/apache+++"
                   :resourceURI "http://sixsq.com/slipstream/1/Module"
                   :type        "COMPONENT"
                   :updated     "2018-10-03T13:19:47.347Z"}})))



#_(deftest test-create-template-from-simple-image
    (is (= (utils/create-deployment-template {:module image-a})
           {:module {:content     {:author           "test"
                                   :commit           "commit image a"
                                   :created          "2018-07-25T10:07:43.309Z"
                                   :id               "module-image/a"
                                   :imageIDs         {:cloud-a 1
                                                      :cloud-b 2
                                                      :cloud-c 3}
                                   :inputParameters  '({:description "Cloud credential ID for managing node deployment"
                                                        :parameter   "credential.id"})
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
                     :resourceURI "http://sixsq.com/slipstream/1/Module"
                     :type        "IMAGE"}}))

    (is (= (utils/create-deployment-template {:module test-module-component-apache})
           {:module
            {:description "Apache web server appliance with custom landing page.",
             :path        "examples/tutorials/service-testing/apache",
             :logo        {:href "https://nuv.la/images/modules-logos/apache-httpd.svg"},
             :content
                          {:loginUser   "root",
                           :updated     "2018-10-03T13:19:47.310Z",
                           :outputParameters
                                        '({:parameter   "ready",
                                           :description "Server ready to recieve connections"}
                                           {:parameter "keypair.name", :description "SSH keypair name used"}
                                           {:parameter "instanceid", :description "Cloud instance id"}
                                           {:parameter "statecustom", :description "Custom state"}
                                           {:parameter "hostname", :description "hostname/ip of the image"}
                                           {:parameter "port", :description "Port", :value "8080"}
                                           {:parameter   "abort",
                                            :description "Machine abort flag, set when aborting"}
                                           {:parameter   "password.ssh",
                                            :description "SSH password if available"}
                                           {:parameter   "url.ssh",
                                            :description "SSH URL to connect to virtual machine"}
                                           {:parameter   "complete",
                                            :description "'true' when current state is completed"}
                                           {:parameter   "url.service",
                                            :description "Optional service URL for virtual machine"}),
                           :created     "2018-10-03T13:19:47.310Z",
                           :imageIDs
                                        {:nuvlabox-scissor1             "67319c6658e857e3f93227acc2d27f50",
                                         :ec2-eu-central-1              "ami-79f51c16",
                                         :nuvlabox-max-born             "67319c6658e857e3f93227acc2d27f50",
                                         :ec2-us-west-1                 "ami-08490c68",
                                         :exoscale-de-fra               "Linux Ubuntu 16.04 LTS 64-bit",
                                         :ec2-ap-northeast-1            "ami-b601ead7",
                                         :nuvlabox-joseph-h-taylor-jr   "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-christiane-nusslein-volhard
                                                                        "67319c6658e857e3f93227acc2d27f50",
                                         :teidehpc-es-tfs1              "251",
                                         :scissor-fr3                   "0",
                                         :nuvlabox-cecil-powell         "67319c6658e857e3f93227acc2d27f50",
                                         :ifb-bird-stack                "509a327d-9976-4915-9013-d3d7ee4b9c8a",
                                         :nuvlabox-demo                 "67319c6658e857e3f93227acc2d27f50",
                                         :ifb-genouest-genostack        "1505dd3c-003d-48a3-8f3e-64ee587736ba",
                                         :nuvlabox-leo-esaki            "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-james-chadwick       "67319c6658e857e3f93227acc2d27f50",
                                         :advania-se1                   "7658761d-b0de-4bbf-87b3-b339a063b27d",
                                         :nuvlabox-felix-bloch          "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-scissor2             "67319c6658e857e3f93227acc2d27f50",
                                         :ec2-sa-east-1                 "ami-09991365",
                                         :cyclone-de1                   "9426b464-adac-4693-b87c-ec628dd744d1",
                                         :nuvlabox-000babccb837         "67319c6658e857e3f93227acc2d27f50",
                                         :exoscale-ch-dk                "Linux Ubuntu 16.04 LTS 64-bit",
                                         :nuvlabox-george-wald          "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-tim-hunt             "67319c6658e857e3f93227acc2d27f50",
                                         :scissor-fr1                   "4",
                                         :nuvlabox-yves-chauvin         "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-christiane-n-volhard "67319c6658e857e3f93227acc2d27f50",
                                         :eo-cesnet-cz1                 "5395",
                                         :ebi-embassy-uk1               "80bc49e5-d926-41a0-bb4a-30470d3bc21d",
                                         :nuvlabox-joseph-e-stiglitz    "67319c6658e857e3f93227acc2d27f50",
                                         :eo-cloudferro-pl1             "a1241c16-4df8-4689-8c75-b770fd547f9e",
                                         :nuvlabox-bertil-ohlin         "67319c6658e857e3f93227acc2d27f50",
                                         :ifb-prabi-girofle             "e66e057f-1d48-4376-8966-e68380d7af84",
                                         :nuvlabox-luis-leloir          "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-jules-bordet         "67319c6658e857e3f93227acc2d27f50",
                                         :ec2-eu-west-2                 "ami-f1d7c395",
                                         :cyfronet-pl1                  "a4c27094-6d68-4532-86e1-68a2b518fa9f",
                                         :ifb-bistro-iphc               "bf240c3b-ae53-4d8b-a998-ba10c20547bc",
                                         :exoscale-at-vie               "Linux Ubuntu 16.04 LTS 64-bit",
                                         :exoscale-ch-gva               "Linux Ubuntu 16.04 LTS 64-bit",
                                         :ec2-eu-west                   "ami-0ae77879",
                                         :nuvlabox-joseph-l-goldstein   "67319c6658e857e3f93227acc2d27f50",
                                         :cyclone-fr2                   "40f32379-de05-4fb8-9e7b-e90a5086afdf",
                                         :nuvlabox-henry-dunant         "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-francis-crick        "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-philipp-lenard       "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-louis-neel           "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-stanley-cohen        "67319c6658e857e3f93227acc2d27f50",
                                         :ec2-ap-southeast-2            "ami-61e3ca02",
                                         :tiscali-it1                   "552ba2ea-2efd-4a47-9e08-a87a75e2dbf4",
                                         :cyclone-it1                   "eb8c957f-408e-43e9-a21c-11ad5d53ce95",
                                         :open-telekom-de1              "a73118a2-10aa-49a1-afb9-5fc480687aa9",
                                         :nuvlabox-arthur-harden        "67319c6658e857e3f93227acc2d27f50",
                                         :cyclone-tb-it1                "eb8c957f-408e-43e9-a21c-11ad5d53ce95",
                                         :scissor-fr2                   "4",
                                         :nuvlabox-theodor-kocher       "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-max-planck           "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-henri-becquerel      "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-carl-cori            "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-albert-einstein      "67319c6658e857e3f93227acc2d27f50",
                                         :ec2-us-east-1                 "ami-f652979b",
                                         :ec2-ap-southeast-1            "ami-e7a67584",
                                         :nuvlabox-joseph-erlanger      "67319c6658e857e3f93227acc2d27f50",
                                         :nuvlabox-joseph-e-murray      "67319c6658e857e3f93227acc2d27f50",
                                         :ifb-core-cloud                "e6c9b4a3-0f1e-4c09-9fde-e09d7e0d8deb",
                                         :ifb-core-pilot                "b142c57c-ccba-43f6-b964-8e195f51ffe4",
                                         :ec2-us-west-2                 "ami-d06a90b0"},
                           :targets
                                        {:postinstall
                                         '("#!/bin/sh -xe\n\napt-get update -y\napt-get install -y apache2"),
                                         :onVmRemove
                                         '("#!/bin/bash\n\ndate\necho \"Scaling action: $SLIPSTREAM_SCALING_ACTION\"\necho \"Scaling node name: $SLIPSTREAM_SCALING_NODE\"\necho \"Scaling node insetance names: $SLIPSTREAM_SCALING_VMS\""),
                                         :onVmAdd
                                         '("#!/bin/bash\n\ndate\necho \"Scaling action: $SLIPSTREAM_SCALING_ACTION\"\necho \"Scaling node name: $SLIPSTREAM_SCALING_NODE\"\necho \"Scaling node insetance names: $SLIPSTREAM_SCALING_VMS\""),
                                         :deployment
                                         '("#!/bin/sh -xe\n\ndefault_site_location='/etc/apache2/sites-available/default'\nif [ ! -f $default_site_location ]; then\n  default_site_location='/etc/apache2/sites-available/000-default.conf'\nfi\n\nhttp_root_location='/var/www/html'\nif [ ! -d $http_root_location ]; then\n  http_root_location='/var/www'\nfi\n\necho 'Hello from Apache deployed by SlipStream!' > $http_root_location/data.txt\n\nservice apache2 stop\nport=$(ss-get port)\n\nsed -i -e 's/^Listen.*$/Listen '$port'/' /etc/apache2/ports.conf\nsed -i -e 's/^NameVirtualHost.*$/NameVirtualHost *:'$port'/' /etc/apache2/ports.conf\nsed -i -e 's/^<VirtualHost.*$/<VirtualHost *:'$port'>/' $default_site_location\nservice apache2 start\nss-set ready true\nurl=\"http://$(ss-get hostname):$port\"\nss-set url.service $url\nss-set ss:url.service $url")},
                           :author      "super",
                           :networkType "public",
                           :id          "module-component/dfd6ee7a-173b-4e5b-996f-b73ab902fabe",
                           :commit      "update ifb-core-cloud flavor",
                           :inputParameters
                                        '({:parameter   "credential.id",
                                           :description "Cloud credential ID for managing node deployment"}),
                           :os          "Ubuntu"},
             :updated     "2018-10-03T13:19:47.347Z",
             :name        "apache",
             :type        "COMPONENT",
             :created     "2018-07-25T10:08:49.035Z",
             :id          "module/b222db88-ee71-47b5-9f0e-0a6eb1177eb7",
             :parentPath  "examples/tutorials/service-testing",
             :acl
                          {:owner {:type "USER", :principal "sixsq"},
                           :rules
                                  [{:principal "sixsq", :right "VIEW", :type "USER"}
                                   {:principal "ADMIN", :right "ALL", :type "ROLE"}]},
             :operations
                          [{:rel "edit", :href "module/b222db88-ee71-47b5-9f0e-0a6eb1177eb7"}
                           {:rel  "delete",
                            :href "module/b222db88-ee71-47b5-9f0e-0a6eb1177eb7"}],
             :resourceURI "http://sixsq.com/slipstream/1/Module",
             :versions
                          [{:author "super",
                            :href   "module-component/074c4c92-bc21-44bd-9b65-516a2db091c4"}
                           {:author "sixsq",
                            :commit "Added Exoscale image parameters",
                            :href   "module-component/1f56fc38-a241-4d0d-a0d5-e12119338715"}
                           {:author "sixsq",
                            :href   "module-component/0ce128a1-487d-449b-9ff8-5b1016f39a13"}
                           {:author "sixsq",
                            :commit "Editing to add the initially configured cloud instances.",
                            :href   "module-component/7ffebdd4-377c-42e5-b160-9bf8be1b22fd"}
                           {:author "sixsq",
                            :commit "Editing to add the initially configured cloud instances.",
                            :href   "module-component/11038934-8962-41fc-8bb6-b6fe60a3faed"}]}}
           ))

    #_(is (=
            {:module
             {:description "Apache web server appliance with custom landing page.",
              :path        "examples/tutorials/service-testing/apache",
              :logo        {:href "https://nuv.la/images/modules-logos/apache-httpd.svg"},
              :content
                           {:loginUser   "root",
                            :updated     "2018-10-03T13:19:47.310Z",
                            :outputParameters
                                         '({:parameter   "ready",
                                            :description "Server ready to recieve connections"}
                                            {:parameter "keypair.name", :description "SSH keypair name used"}
                                            {:parameter "instanceid", :description "Cloud instance id"}
                                            {:parameter "statecustom", :description "Custom state"}
                                            {:parameter "hostname", :description "hostname/ip of the image"}
                                            {:parameter "port", :description "Port", :value "8080"}
                                            {:parameter   "abort",
                                             :description "Machine abort flag, set when aborting"}
                                            {:parameter   "password.ssh",
                                             :description "SSH password if available"}
                                            {:parameter   "url.ssh",
                                             :description "SSH URL to connect to virtual machine"}
                                            {:parameter   "complete",
                                             :description "'true' when current state is completed"}
                                            {:parameter   "url.service",
                                             :description "Optional service URL for virtual machine"}),
                            :created     "2018-10-03T13:19:47.310Z",
                            :imageIDs
                                         {:nuvlabox-scissor1             "67319c6658e857e3f93227acc2d27f50",
                                          :ec2-eu-central-1              "ami-79f51c16",
                                          :nuvlabox-max-born             "67319c6658e857e3f93227acc2d27f50",
                                          :ec2-us-west-1                 "ami-08490c68",
                                          :exoscale-de-fra               "Linux Ubuntu 16.04 LTS 64-bit",
                                          :ec2-ap-northeast-1            "ami-b601ead7",
                                          :nuvlabox-joseph-h-taylor-jr   "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-christiane-nusslein-volhard
                                                                         "67319c6658e857e3f93227acc2d27f50",
                                          :teidehpc-es-tfs1              "251",
                                          :scissor-fr3                   "0",
                                          :nuvlabox-cecil-powell         "67319c6658e857e3f93227acc2d27f50",
                                          :ifb-bird-stack                "509a327d-9976-4915-9013-d3d7ee4b9c8a",
                                          :nuvlabox-demo                 "67319c6658e857e3f93227acc2d27f50",
                                          :ifb-genouest-genostack        "1505dd3c-003d-48a3-8f3e-64ee587736ba",
                                          :nuvlabox-leo-esaki            "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-james-chadwick       "67319c6658e857e3f93227acc2d27f50",
                                          :advania-se1                   "7658761d-b0de-4bbf-87b3-b339a063b27d",
                                          :nuvlabox-felix-bloch          "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-scissor2             "67319c6658e857e3f93227acc2d27f50",
                                          :ec2-sa-east-1                 "ami-09991365",
                                          :cyclone-de1                   "9426b464-adac-4693-b87c-ec628dd744d1",
                                          :nuvlabox-000babccb837         "67319c6658e857e3f93227acc2d27f50",
                                          :exoscale-ch-dk                "Linux Ubuntu 16.04 LTS 64-bit",
                                          :nuvlabox-george-wald          "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-tim-hunt             "67319c6658e857e3f93227acc2d27f50",
                                          :scissor-fr1                   "4",
                                          :nuvlabox-yves-chauvin         "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-christiane-n-volhard "67319c6658e857e3f93227acc2d27f50",
                                          :eo-cesnet-cz1                 "5395",
                                          :ebi-embassy-uk1               "80bc49e5-d926-41a0-bb4a-30470d3bc21d",
                                          :nuvlabox-joseph-e-stiglitz    "67319c6658e857e3f93227acc2d27f50",
                                          :eo-cloudferro-pl1             "a1241c16-4df8-4689-8c75-b770fd547f9e",
                                          :nuvlabox-bertil-ohlin         "67319c6658e857e3f93227acc2d27f50",
                                          :ifb-prabi-girofle             "e66e057f-1d48-4376-8966-e68380d7af84",
                                          :nuvlabox-luis-leloir          "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-jules-bordet         "67319c6658e857e3f93227acc2d27f50",
                                          :ec2-eu-west-2                 "ami-f1d7c395",
                                          :cyfronet-pl1                  "a4c27094-6d68-4532-86e1-68a2b518fa9f",
                                          :ifb-bistro-iphc               "bf240c3b-ae53-4d8b-a998-ba10c20547bc",
                                          :exoscale-at-vie               "Linux Ubuntu 16.04 LTS 64-bit",
                                          :exoscale-ch-gva               "Linux Ubuntu 16.04 LTS 64-bit",
                                          :ec2-eu-west                   "ami-0ae77879",
                                          :nuvlabox-joseph-l-goldstein   "67319c6658e857e3f93227acc2d27f50",
                                          :cyclone-fr2                   "40f32379-de05-4fb8-9e7b-e90a5086afdf",
                                          :nuvlabox-henry-dunant         "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-francis-crick        "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-philipp-lenard       "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-louis-neel           "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-stanley-cohen        "67319c6658e857e3f93227acc2d27f50",
                                          :ec2-ap-southeast-2            "ami-61e3ca02",
                                          :tiscali-it1                   "552ba2ea-2efd-4a47-9e08-a87a75e2dbf4",
                                          :cyclone-it1                   "eb8c957f-408e-43e9-a21c-11ad5d53ce95",
                                          :open-telekom-de1              "a73118a2-10aa-49a1-afb9-5fc480687aa9",
                                          :nuvlabox-arthur-harden        "67319c6658e857e3f93227acc2d27f50",
                                          :cyclone-tb-it1                "eb8c957f-408e-43e9-a21c-11ad5d53ce95",
                                          :scissor-fr2                   "4",
                                          :nuvlabox-theodor-kocher       "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-max-planck           "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-henri-becquerel      "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-carl-cori            "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-albert-einstein      "67319c6658e857e3f93227acc2d27f50",
                                          :ec2-us-east-1                 "ami-f652979b",
                                          :ec2-ap-southeast-1            "ami-e7a67584",
                                          :nuvlabox-joseph-erlanger      "67319c6658e857e3f93227acc2d27f50",
                                          :nuvlabox-joseph-e-murray      "67319c6658e857e3f93227acc2d27f50",
                                          :ifb-core-cloud                "e6c9b4a3-0f1e-4c09-9fde-e09d7e0d8deb",
                                          :ifb-core-pilot                "b142c57c-ccba-43f6-b964-8e195f51ffe4",
                                          :ec2-us-west-2                 "ami-d06a90b0"},
                            :targets
                                         {:postinstall
                                          '("#!/bin/sh -xe\n\napt-get update -y\napt-get install -y apache2"),
                                          :onVmRemove
                                          '("#!/bin/bash\n\ndate\necho \"Scaling action: $SLIPSTREAM_SCALING_ACTION\"\necho \"Scaling node name: $SLIPSTREAM_SCALING_NODE\"\necho \"Scaling node insetance names: $SLIPSTREAM_SCALING_VMS\""),
                                          :onVmAdd
                                          '("#!/bin/bash\n\ndate\necho \"Scaling action: $SLIPSTREAM_SCALING_ACTION\"\necho \"Scaling node name: $SLIPSTREAM_SCALING_NODE\"\necho \"Scaling node insetance names: $SLIPSTREAM_SCALING_VMS\""),
                                          :deployment
                                          '("#!/bin/sh -xe\n\ndefault_site_location='/etc/apache2/sites-available/default'\nif [ ! -f $default_site_location ]; then\n  default_site_location='/etc/apache2/sites-available/000-default.conf'\nfi\n\nhttp_root_location='/var/www/html'\nif [ ! -d $http_root_location ]; then\n  http_root_location='/var/www'\nfi\n\necho 'Hello from Apache deployed by SlipStream!' > $http_root_location/data.txt\n\nservice apache2 stop\nport=$(ss-get port)\n\nsed -i -e 's/^Listen.*$/Listen '$port'/' /etc/apache2/ports.conf\nsed -i -e 's/^NameVirtualHost.*$/NameVirtualHost *:'$port'/' /etc/apache2/ports.conf\nsed -i -e 's/^<VirtualHost.*$/<VirtualHost *:'$port'>/' $default_site_location\nservice apache2 start\nss-set ready true\nurl=\"http://$(ss-get hostname):$port\"\nss-set url.service $url\nss-set ss:url.service $url")},
                            :author      "super",
                            :networkType "public",
                            :id          "module-component/dfd6ee7a-173b-4e5b-996f-b73ab902fabe",
                            :commit      "update ifb-core-cloud flavor",
                            :inputParameters
                                         '({:parameter   "credential.id",
                                            :description "Cloud credential ID for managing node deployment"}),
                            :os          "Ubuntu"},
              :updated     "2018-10-03T13:19:47.347Z",
              :name        "apache",
              :type        "COMPONENT",
              :created     "2018-07-25T10:08:49.035Z",
              :id          "module/b222db88-ee71-47b5-9f0e-0a6eb1177eb7",
              :parentPath  "examples/tutorials/service-testing",
              :acl
                           {:owner {:type "USER", :principal "sixsq"},
                            :rules
                                   [{:principal "sixsq", :right "VIEW", :type "USER"}
                                    {:principal "ADMIN", :right "ALL", :type "ROLE"}]},
              :operations
                           [{:rel "edit", :href "module/b222db88-ee71-47b5-9f0e-0a6eb1177eb7"}
                            {:rel  "delete",
                             :href "module/b222db88-ee71-47b5-9f0e-0a6eb1177eb7"}],
              :resourceURI "http://sixsq.com/slipstream/1/Module",
              :versions
                           [{:author "super",
                             :href   "module-component/074c4c92-bc21-44bd-9b65-516a2db091c4"}
                            {:author "sixsq",
                             :commit "Added Exoscale image parameters",
                             :href   "module-component/1f56fc38-a241-4d0d-a0d5-e12119338715"}
                            {:author "sixsq",
                             :href   "module-component/0ce128a1-487d-449b-9ff8-5b1016f39a13"}
                            {:author "sixsq",
                             :commit "Editing to add the initially configured cloud instances.",
                             :href   "module-component/7ffebdd4-377c-42e5-b160-9bf8be1b22fd"}
                            {:author "sixsq",
                             :commit "Editing to add the initially configured cloud instances.",
                             :href   "module-component/11038934-8962-41fc-8bb6-b6fe60a3faed"}]}}
            )))