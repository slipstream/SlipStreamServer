(ns com.sixsq.slipstream.ssclj.db.database-binding-delete-test
  (:refer-clojure :exclude [update])
  (:require    
    [com.sixsq.slipstream.ssclj.db.database-binding         :as dbb] 
    [com.sixsq.slipstream.ssclj.db.filesystem-binding-utils :refer [serialize]]
    [korma.core                                             :refer :all]    
    [korma.db                                               :refer [transaction]]    
    [expectations                                           :refer :all]
    [clojure.tools.logging                                  :as log]))

(def db (dbb/get-instance))

(defentity  resources)
(delete     resources)
(log/info "All resources deleted")

(def data {:id "Thing/456" :name "alfred" :age 23})

(.add db "Thing" data)
(let [response-delete (.delete db data)]
	;; When we delete an existing data
	(expect 204 (:status response-delete)))
	
(try
  (.retrieve db 123)
  (expect false "should have been caught")
  (catch Exception e
    (log/info "caught exception: class=" (class e))))
	; Then the data can no longer be retrieved
