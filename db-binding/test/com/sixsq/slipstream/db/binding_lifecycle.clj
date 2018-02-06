(ns com.sixsq.slipstream.db.binding-lifecycle
  (:require
    [clojure.test :refer [deftest is are]]
    [com.sixsq.slipstream.db.binding :as db]))

(defn check-binding-lifecycle [db-impl]
  (with-open [db db-impl]

    ;; create an entry in the database
    (let [my-id "my-collection/my-uuid"
          my-data {:id my-id, :one 1, :two "2"}
          response (db/add db my-data nil)]
      (is (= 201 (:status response)))
      (is (= my-id (get-in response [:headers "Location"])))

      ;; ensure that the entry can be retrieved
      (let [retrieved-data (db/retrieve db my-id nil)]
        (is (= my-data retrieved-data)))

      ;; check that it shows up in a query
      (let [[query-meta query-hits] (db/query db "my-collection" nil)]
        (is (= 1 (:count query-meta)))
        (is (= my-data (first query-hits))))

      ;; add a second entry
      (let [my-id-2 "my-collection/my-uuid-2"
            my-data-2 {:id my-id-2, :one 1, :two "2"}
            response (db/add db my-data-2 nil)]
        (is (= 201 (:status response)))
        (is (= my-id-2 (get-in response [:headers "Location"])))

        ;; ensure that is can be retrieved (and flush index for elasticsearch)
        (let [retrieved-data (db/retrieve db my-id-2 nil)]
          (is (= my-data-2 retrieved-data)))

        ;; check that query has another entry
        (let [[query-meta query-hits] (db/query db "my-collection" nil)]
          (is (= 2 (:count query-meta)))
          (is (= #{my-id my-id-2} (set (map :id query-hits)))))

        ;; adding the same entry again must fail
        (let [response (db/add db {:id my-id} nil)]
          (is (= 409 (:status response))))

        ;; update the entry
        (let [updated-data (assoc my-data :two "3")
              response (db/edit db updated-data nil)]
          (is (= 200 (:status response)))

          ;; make sure that the update was applied
          (let [retrieved-data (db/retrieve db my-id nil)]
            (is (= updated-data retrieved-data)))

          ;; delete the first entry
          (let [response (db/delete db updated-data nil)]
            (is (= 200 (:status response))))

          ;; delete the second entry
          (let [response (db/delete db {:id my-id-2} nil)]
            (is (= 200 (:status response))))

          ;; deleting the first one a second time should give a 404
          (let [response (db/delete db updated-data nil)]
            (is (= 404 (:status response)))))

        ;; also retrieving it should do the same
        (let [response (db/retrieve db my-id nil)]
          (is (= 404 (:status response))))))))
