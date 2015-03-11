(ns com.sixsq.slipstream.ssclj.resources.event-schema-test
  (require
    [schema.core :as s]
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.resources.event :refer :all]
    [com.sixsq.slipstream.ssclj.resources.common.schema :as c]
    [com.sixsq.slipstream.ssclj.resources.common.crud :as crud]
    ))
 
(let [  
  event-timestamp "2015-01-16T08:05:00.0Z"
  valid-event { 
                :acl {:owner {:type "USER" :principal "joe"}
                      :rules [{:type "ROLE" :principal "ANON" :right "ALL"}]}
                
                :resourceURI resource-uri

                :timestamp event-timestamp
                :content  { :resource {:href "Run/45614147-aed1-4a24-889d-6365b0b1f2cd"}
                            :state "Started" } 
                :type "state"
                :severity "critical"
                }]
  (expect nil? (s/check Event valid-event))
  (expect valid-event (crud/validate valid-event)))

