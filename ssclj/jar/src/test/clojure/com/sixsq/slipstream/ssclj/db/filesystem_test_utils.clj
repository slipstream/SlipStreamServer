(ns com.sixsq.slipstream.ssclj.db.filesystem-test-utils
  (:require
    [fs.core :as fs]))

(defn- remove-db
  []
  (if (fs/exists? "testdb")
    (fs/delete-dir "testdb")))

(defn flush-db-fixture
  [f]
  (remove-db)
  (f))

(defn temp-db-fixture
  [f]
  #_(remove-db)
  (f))
