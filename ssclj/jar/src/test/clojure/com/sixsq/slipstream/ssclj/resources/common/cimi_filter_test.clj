(ns com.sixsq.slipstream.ssclj.resources.common.cimi-filter-test
  (:require
    [com.sixsq.slipstream.ssclj.resources.common.cimi-filter  :refer :all]
    [clojure.test                                             :refer :all]))

(def event1
  {:content
            {:state    "init"
             :resource {:href "run/1234"}}
   :updated "2015-05-15T14:59:16.059Z"
   :type    "state"
   :created "2015-05-15T14:59:16.059Z"
   :id      "Event/06779f74-e99c-44c8-87d5-0b9d5e7ceedd"
   })

(def event2
  {:content
            {:state    "init"
             :resource {:href "run/7890"}}
   :updated "2016-06-15T14:59:16.059Z"
   :type    "critical"
   :created "2016-05-15T14:59:16.059Z"
   :id      "Event/12312312312312312323123"
   })

(def events [event1 event2])

(deftest filter-cimi-simple-expression
  "Dotted notation is used to locate nested attribute"
  (is (= [event1] (cimi-filter events "type='state'")))
  (is (= [event2] (cimi-filter events "type='critical'")))
  (is (= []       (cimi-filter events "type='unknown'"))))

(deftest filter-or
  (is (= [event1]   (cimi-filter events "type='state' or type='BB'")))
  (is (= [event2]   (cimi-filter events "type='critical' or type='BB'")))
  (is (= []         (cimi-filter events "type='AA' or type='BB'")))
  (is (= events     (cimi-filter events "type='state' or type='critical'")))
  (is (= events     (cimi-filter events "(type='state') or (type='critical')")))
  (is (= events     (cimi-filter events "content/resource/href='run/1234' or type='critical'")))
  (is (= events     (cimi-filter events "content/resource/href='run/1234' or type='critical' or type='state'"))))

(deftest filter-cimi-nested-expression
  "Dotted notation is used to locate nested attribute"
  (is (= [event1] (cimi-filter events "content/resource/href='run/1234'")))
  (is (= [event2] (cimi-filter events "content/resource/href='run/7890'")))
  (is (= []       (cimi-filter events "content/resource/href='run/1111'"))))

(deftest filter-cimi-expression-quotes
  "Values must be simple or double quoted, and quotation must be consistent"
  (is (= [event1] (cimi-filter events "content/resource/href='run/1234'")))
  (is (= [event2] (cimi-filter events "content/resource/href=\"run/7890\"")))

  (is (thrown? IllegalArgumentException (doall (cimi-filter events "content/resource/href=run/7890"))))
  (is (thrown? IllegalArgumentException (doall (cimi-filter events "content/resource/href=\"run/7890'"))))
  (is (thrown? IllegalArgumentException (doall (cimi-filter events "content/resource/href='run/7890\"")))))

(deftest filter-cimi-checks-attribute-presence
  (is (thrown? IllegalArgumentException (doall (cimi-filter events "abc=123")))))

(deftest filter-cimi-various-operators
  (is (= [event2] (cimi-filter events "type!='state'")))
  (is (= [event1] (cimi-filter events "type!='critical'")))
  (is (= events   (cimi-filter events "type!='unknown'"))))

(deftest filter-cimi-date-comparisons
  (is (= []       (cimi-filter events "created<'2015-05-15'")))
  (is (= events   (cimi-filter events "created<'2017-05-15'")))
  (is (= [event1] (cimi-filter events "created<'2016-01-01'")))

  (is (= events   (cimi-filter events "created>'2014-01-01'")))
  (is (= [event2] (cimi-filter events "created>'2016-01-01'"))))

(deftest filter-cimi-multiple-ands
  (is (= events   (cimi-filter events "content/state='init'")))
  (is (= [event1] (cimi-filter events "content/state='init' and type='state'")))
  (is (= [event2] (cimi-filter events "content/state='init' and type!='state'")))
  (is (= [event2] (cimi-filter events "content/state='init' and type='critical' and updated>'2016-01-01'")))
  (is (= [event2] (cimi-filter events "content/state='init' and type='critical' and updated>'2016-01-01' and created>'2015-01-01'")))
  (is (= []
         (cimi-filter events "content/state='init' and type='critical' and updated>'2016-01-01' and created<'2015-01-01'"))))

(deftest filter-and-higher-priority-than-or
  (is (= [event2] (cimi-filter events
    "id='Event/12312312312312312323123' and type='state' or type='critical'")))
  (is (= [event1] (cimi-filter events
    "id='Event/06779f74-e99c-44c8-87d5-0b9d5e7ceedd' or type='state' and type='critical'"))))

