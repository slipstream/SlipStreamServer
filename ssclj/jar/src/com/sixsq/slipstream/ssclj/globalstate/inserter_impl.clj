(ns com.sixsq.slipstream.ssclj.globalstate.inserter-impl
  (:require
    [com.sixsq.slipstream.db.es.binding :as esb]
    [com.sixsq.slipstream.db.impl :as db])
  (:gen-class)
  )

(defn do-insert
  [& args]
  (println "inserting")
  )

(defn -main
  "See tests for examples on how to call from clojure REPL"
  [& args]
  (db/set-impl! (esb/get-instance))
  (esb/set-client! (esb/create-client))
  (apply do-insert args))
