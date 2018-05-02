(ns com.sixsq.slipstream.ssclj.resources.spec.util.spec-tools-live-test
  (:require [clojure.test :refer :all]
            [com.sixsq.slipstream.dbtest.es.spandex-utils :as spu]
            [com.sixsq.slipstream.ssclj.resources.spec.util.spec-tools :as t]
            [com.sixsq.slipstream.ssclj.resources.spec.common :as c]
            [com.sixsq.slipstream.ssclj.resources.spec.util.es-tools :as et]
            [qbits.spandex :as spandex])
  (:import (java.util UUID))
  )


(defn check-mappings [index-name mapping]
  (let [rest-map (spu/provide-mock-rest-client)
        doc-id (str (UUID/randomUUID))
        doc {:some {:fake "data"}}
        ]
    (with-open [client (:client rest-map)]
      (when (spu/cluster-ready? client)
        (spu/index-create-with-mapping client index-name mapping)
        (try

          (spu/index-refresh client index-name)

          (is (->> (spandex/request client
                                    {:url    [index-name type doc-id]
                                     :method :post
                                     :body   doc})
                   :status
                   (contains? #{200 201})))

          (is (-> (spandex/request client
                                   {:url    [index-name type doc-id]
                                    :method :get})
                  :body
                  :_source
                  (= doc)))

          (is (-> (spandex/request client
                                   {:url         [index-name type doc-id]
                                    :method      :get
                                    :keywordize? false})
                  :body
                  (get "_source")
                  (= (clojure.walk/stringify-keys doc))))

          (catch Exception e
            ((clojure.pprint/pprint (str "[ELG]") e))
            )

          (finally
            (try
              (spu/index-delete client index-name)
              (catch Exception _))
            ))))))


(deftest live-mappings

  (do
    #_(check-mappings "my_index" {})
    (check-mappings "my_index2" (t/spec->es-mapping :cimi/metering))
    )

  )

(def mmorig {:mappings {:_doc {:properties {:connector     {:type       "object",
                                                            :properties {"href" {:type "string"}},
                                                            :title      "cimi.virtual-machine/connector"},
                                            :description   {:type "string"},
                                            :properties    {:type                 "object",
                                                            :additionalProperties {:type "string"},
                                                            :title                "com.sixsq.slipstream.ssclj.resources.spec.common/properties"},
                                            :ip            {:type "string"},
                                            :credentials   {:type "object", :properties {"href" {:type "string"}}},
                                            :updated       {:type "string"},
                                            :name          {:type "string"},
                                            :created       {:type "string"},
                                            :state         {:type "string"},
                                            :instanceID    {:type "string"},
                                            :id            {:type "string"},
                                            :deployment    {:type       "object",
                                                            :properties {"href" {:type "string"}},
                                                            :title      "cimi.virtual-machine/deployment"},
                                            :acl           {:type       "object",
                                                            :properties {"owner" {:type       "object",
                                                                                  :properties {"principal" {:type "string"},
                                                                                               "type"      {:enum ["USER" "ROLE"]}}},
                                                                         "rules" {:type       "object",
                                                                                  :properties {"principal" {:type "string"},
                                                                                               "type"      {:enum ["USER" "ROLE"]},
                                                                                               "right"     {:enum ["DELETE"
                                                                                                                   "MANAGE"
                                                                                                                   "VIEW_DATA"
                                                                                                                   "EDIT_META"
                                                                                                                   "MODIFY"
                                                                                                                   "VIEW"
                                                                                                                   "VIEW_META"
                                                                                                                   "VIEW_ACL"
                                                                                                                   "ALL"
                                                                                                                   "EDIT_DATA"
                                                                                                                   "EDIT_ACL"]}}}}},
                                            :operations    {:type       "object",
                                                            :properties {"href" {:type "string"}, "rel" {:type "string"}}},
                                            :resourceURI   {:type "string"},
                                            :serviceOffer  {:type       "object",
                                                            :properties {"href" {:type "string"}},
                                                            :title      "cimi.virtual-machine/serviceOffer"},
                                            :snapshot-time {:type "string"}}}}})


(def mm  {:mappings {:_doc {:properties {:connector     {:type       "object",
                                                                    :properties {"href" {:type "text"}},
                                                                    :title      "cimi.virtual-machine/connector"},
                                                    :description   {:type "text"},
                                                    :properties    {:type                 "object",
                                                                    :additionalProperties {:type "text"},
                                                                    :title                "com.sixsq.slipstream.ssclj.resources.spec.common/properties"},
                                                    :ip            {:type "text"},
                                                    :credentials   {:type "object", :properties {"href" {:type "text"}}},
                                                    :updated       {:type "text"},
                                                    :name          {:type "text"},
                                                    :created       {:type "text"},
                                                    :state         {:type "text"},
                                                    :instanceID    {:type "text"},
                                                    :id            {:type "text"},
                                                    :deployment    {:type       "object",
                                                                    :properties {"href" {:type "text"}},
                                                                    :title      "cimi.virtual-machine/deployment"},
                                                    :acl           {:type       "object",
                                                                    :properties {"owner" {:type       "object",
                                                                                          :properties {"principal" {:type "text"},
                                                                                                       "type"      {:type "text"}}},
                                                                                 "rules" {:type       "object",
                                                                                          :properties {"principal" {:type "text"},
                                                                                                       "type"      {:type "text"},
                                                                                                       "right"     {:type "text"}}}}},
                                                    :operations    {:type       "object",
                                                                    :properties {"href" {:type "text"}, "rel" {:type "text"}}},
                                                    :resourceURI   {:type "text"},
                                                    :serviceOffer  {:type       "object",
                                                                    :properties {"href" {:type "text"}},
                                                                    ;;:title      "cimi.virtual-machine/serviceOffer"
                                                                    },
                                                    :snapshot-time {:type "text"}}}}})


(def mm2 {:mappings {:_doc {:properties {:name        {:type       "object",
                                                       :properties {:field_1 {:type "text"}, :field_2 {:type "text"}}},
                                         :age         {:type "long"},
                                         :joiningDate {:type "date"}}}}}
  )