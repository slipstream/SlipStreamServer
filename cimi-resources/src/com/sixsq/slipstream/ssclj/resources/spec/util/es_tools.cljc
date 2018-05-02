(ns com.sixsq.slipstream.ssclj.resources.spec.util.es-tools
  (:require
    [qbits.spandex :as s]
    )
  )


(defn create-index [index propertiesMap]
  "Create an index with provided properties map"
  (with-open [client (s/client {:hosts ["http://127.0.0.1:9200"]})]
    (try
      (s/request client
                 {:url    [index]
                  :method :put
                  ;;:body   {:mappings {:_doc  propertiesMap }}
                  :body {:mappings {:_doc {:properties {}}}}
                  })
      (catch Exception e
        (clojure.pprint/pprint e)
        ))
    ))


(defn create-index2 [index mapping]
  "Create an index with provided properties map"
  (with-open [client (s/client {:hosts ["http://127.0.0.1:9200"]})]
    (try
      (s/request client
                 {:url    [index]
                  :method :put
                  ;;:body   {:mappings {:_doc  propertiesMap }}
                  :body mapping}
                  )
      (catch Exception e
        (clojure.pprint/pprint e)
        ))
    ))

(defn remove-index [index]
  (with-open [client (s/client {:hosts ["http://127.0.0.1:9200"]})]

    (try
      (s/request client
                 {:url    [index]
                  :method :delete})
      (catch Exception e
        (clojure.pprint/pprint e)
        ))

    )

  )
