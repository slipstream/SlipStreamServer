(ns com.sixsq.slipstream.db.es-rest.test-utils
  (:require
    [qbits.spandex :as spandex]))


(def dynamic-templates [{:strings {:match              "*"
                                   :match_mapping_type "string"
                                   :mapping            {:type "keyword"}}}])


(def index-template {:index_patterns ["*"]
                     :mappings       {:_doc {:dynamic_templates dynamic-templates}}})


(defn initialize-db
  [client]
  (spandex/request client {:url    ["_template" "slipstream-defaults"]
                           :method :put
                           :body   index-template}))
