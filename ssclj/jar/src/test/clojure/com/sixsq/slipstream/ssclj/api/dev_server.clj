(ns com.sixsq.slipstream.ssclj.api.dev-server
  (:require
    [com.sixsq.slipstream.ssclj.app.server            :as server]
    [com.sixsq.slipstream.ssclj.resources.seeds.event :as es]
    [com.sixsq.slipstream.ssclj.usage.seeds.usages    :as us]))

(def ^:private ^:dynamic *server-port* nil)

(defonce server
  (delay
    (println (str "Server started at localhost:" *server-port*))
    (server/start *server-port*)))

(defn bootstrap
  [& {:keys [port events summaries user summaries clouds]
      :or {port       8201
           events     45
           summaries  45
           clouds     ["aws" "exo"]
           user       :bob}}]
    (binding [*server-port* port] @server)
    (println (str "Seeding " events " events for user '" (name user) "'..." ))
    (es/seed! events user)
    (println (str "Seeding " summaries " summaries for clouds '" clouds "' for user '" (name user) "'..."))
    (us/seed-summaries! summaries user clouds))

(println)
(println "Type (api/bootstrap) to start a server with 45 events and 45 summaries for user 'bob'")
(println "For customised values use named parameters like:")
(println "     :user      \"anotheruser\"")
(println "     :events    100")
(println "     :summaries 100")
(println "     :clouds    [\"onecloud\" \"anothercloud\"]")
(println)
