(def +version+ "3.44-SNAPSHOT")

;; FIXME: Provide HTTPS access to Nexus.
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject
  com.sixsq.slipstream/SlipStreamMigration-jar
  "3.44-SNAPSHOT"
  :license
  {"Apache 2.0" "http://www.apache.org/licenses/LICENSE-2.0.txt"}
  
  :plugins [[lein-parent "0.3.2"]]
  
  :parent-project {:coords  [com.sixsq.slipstream/parent "3.44-SNAPSHOT"]
                   :inherit [:min-lein-version
                             :managed-dependencies
                             :repositories
                             :deploy-repositories]}

  :source-paths ["src", "resources"]

  :pom-location "target/"

  :dependencies [[korma]
                 [org.apache.curator/curator-test :scope "compile"]
                 [com.cemerick/url]
                 [com.sixsq.slipstream/SlipStreamCljResources-jar]]

  :profiles {
             :provided {:aot [com.sixsq.slipstream.ssclj.util.userparamsdesc
                              com.sixsq.slipstream.ssclj.migrate.user-cred]}
             :test {
                    :env            {:config-name      "config-hsqldb.edn"
                                     :auth-private-key "test-resources/auth_privkey.pem"
                                     :auth-public-key  "test-resources/auth_pubkey.pem"}
                    :aot            :all}
             :dev      {:dependencies [[com.sixsq.slipstream/slipstream-ring-container]]}})
