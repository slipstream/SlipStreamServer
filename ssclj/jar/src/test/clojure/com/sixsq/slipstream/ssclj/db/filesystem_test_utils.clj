(ns com.sixsq.slipstream.ssclj.db.filesystem-test-utils
  (:require
    [com.sixsq.slipstream.ssclj.app.server :refer [create-root]]
    [com.sixsq.slipstream.ssclj.middleware.base-uri :refer [wrap-base-uri]]
    [com.sixsq.slipstream.ssclj.middleware.exception-handler :refer [wrap-exceptions]]
    [compojure.core :as cc]
    [clojure.test :refer [is]]
    [clojure.pprint :refer [pprint]]
    [clojure.tools.logging :as log]
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
