(ns com.sixsq.slipstream.ssclj.usage.projector-test
  (:require    
    [com.sixsq.slipstream.ssclj.usage.projector :as p]   
    [com.sixsq.slipstream.ssclj.usage.utils :as u] 
    [clojure.test :refer :all]))

(def u1 { :id "joe"
          :event :start 
          :timestamp (u/to-time "2015-01-16T08:20:00.0Z")
          :dimensions { :nb-cpu 2
                        :ram-GB 8
                        :disk-GK 100}})

