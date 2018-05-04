(ns com.sixsq.slipstream.ssclj.util.es-mapping-test
  (:require
    [clojure.test :refer [deftest testing is are]]
    [com.sixsq.slipstream.ssclj.resources.spec.common :as common]
    [com.sixsq.slipstream.db.es.common.es-mapping :as t]))


(deftest common-schema

  (are [spec expected] (= (t/transform spec) expected)
                       common/common-attrs {}
                       common/create-attrs {}
                       common/template-attrs {}

                       ::common/acl {:type       "object",
                                     :properties {"owner" {:type       "object",
                                                           :properties {"principal" {:type "keyword"}, "type" {:type "keyword"}}},
                                                  "rules" {:type       "object",
                                                           :properties {"principal" {:type "keyword"},
                                                                        "type"      {:type "keyword"},
                                                                        "right"     {:type "keyword"}}}}}

                       ::common/operations {:type "object", :properties {"href" {:type "keyword"}, "rel" {:type "keyword"}}}


                       ::common/properties {:type                 "object",
                                            :properties {:type "keyword"}}

                       ::common/id {:type "keyword"}
                       ::common/resourceURI {:type "keyword"}
                       ::common/created {:type "date", :format "strict_date_optional_time||epoch_millis"}
                       ::common/updated {:type "date", :format "strict_date_optional_time||epoch_millis"}
                       ::common/name {:type "keyword"}
                       ::common/description {:type "text"}
                       ::common/href {:type "keyword"}
                       ::common/resource-link {:type "object", :properties {"href" {:type "keyword"}}}
                       ::common/resource-links {:type "object", :properties {"href" {:type "keyword"}}}
                       ::common/operation {:type "object", :properties {"href" {:type "keyword"}, "rel" {:type "keyword"}}}
                       ::common/kw-or-str {:type "keyword"}
                       ))
