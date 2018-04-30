(ns com.sixsq.slipstream.db.es-rest.test-utils
  (:require
    [qbits.spandex :as spandex]))


;;
;; Ensures that string fields will be treated as keywords by default.
;;
(def dynamic-templates {:dynamic_templates
                        [{:strings {:match              "*"
                                    :match_mapping_type "string"
                                    :mapping            {:type  "string"
                                                         :index "not_analyzed"}}}]})


;;
;; This must change when moving from ES5 to ES6.  The 'template'
;; field should be removed and replaced with 'index_patterns'.
;; The '_default_' type should be removed and replaced with "_doc".
;;
(def index-template {:template       "*"
                     ;:index_patterns ["*"]
                     :mappings       {:_default_ dynamic-templates
                                      ;:_doc      dynamic-templates
                                      }})


(defn initialize-db
  [client]
  (spandex/request client {:url    ["_template" "slipstream-defaults"]
                           :method :put
                           :body   index-template}))
