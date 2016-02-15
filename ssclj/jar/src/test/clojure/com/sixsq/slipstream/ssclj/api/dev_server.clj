(ns com.sixsq.slipstream.ssclj.api.dev-server
  (:require
    [korma.core :as kc]
    [com.sixsq.slipstream.ssclj.database.korma-helper :as kh]
    [com.sixsq.slipstream.ssclj.app.server :as server]
    [com.sixsq.slipstream.ssclj.resources.seeds.event :as es]
    [com.sixsq.slipstream.ssclj.usage.seeds.usages :as us]))

(def ^:private ^:dynamic *server-port* nil)

(defonce server
         (delay
           (println (str "Server started at localhost:" *server-port*))
           (server/start *server-port*)))

(kc/defentity resources (kc/database kh/korma-api-db))
(kc/defentity usage_summaries (kc/database kh/korma-api-db))

(defn db-content
  []
  {:resource  (->> (kc/select resources)
                   (map :id)
                   (map (partial re-find #"(.*)/.*"))
                   (map second)
                   frequencies)
   :summaries (->> (kc/select usage_summaries)
                   (map :user)
                   frequencies)})

(defn empty-db?
  []
  (->> (db-content) vals (every? #{{}})))

(defn bootstrap
  [& {:keys [port events summaries user summaries clouds clean]
      :or   {port      8201
             events    45
             summaries 45
             clouds    ["aws" "exo"]
             user      :bob
             clean     true}}]
  (binding [*server-port* port] @server)
  (println (str "Seeding " events " events for user '" (name user) "'..."))
  (when clean
    (println "All DB entries will be deleted. Use (bootstrap :clean false) to avoid it."))
  (es/seed! events user :clean clean)
  (println (str "Seeding " summaries " summaries for clouds '" clouds "' for user '" (name user) "'..."))
  (us/seed-summaries! summaries user clouds :clean clean)
  (println "The current state of the DB is:")
  (clojure.pprint/pprint (db-content)))

(println)
(println "Type (api/bootstrap) to start a server with 45 events and 45 summaries for user 'bob'")
(println "For customised values use named parameters like:")
(println "     :user      \"anotheruser\"")
(println "     :events    100")
(println "     :summaries 100")
(println "     :clouds    [\"onecloud\" \"anothercloud\"]")
(println)
