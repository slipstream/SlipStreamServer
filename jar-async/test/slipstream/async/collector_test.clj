(ns slipstream.async.collector-test
  (:require    
    [clojure.test                 :refer :all]
    [slipstream.async.collector   :refer :all]))

(deftest test-add-increasing-space
  (is (= [["joe" "exo" 0] ["joe" "aws" 10] ["mike" "exo" 20]]
         (add-increasing-space [["joe" "exo"] ["joe" "aws"] ["mike" "exo"]] 10))))
  


