(ns com.sixsq.slipstream.auth.database.korma-helper
  (:refer-clojure :exclude [update])
  (:require
    [korma.db               :refer [defdb]]
    [clojure.tools.logging  :as log]
    [com.sixsq.slipstream.auth.conf.config :as cf]))

(defn db-spec
  []
  (cf/property-value :auth-db))

(def korma-init-done
  (delay
    (let [current-db-spec (db-spec)]
      (log/info (format "Creating korma database %s" current-db-spec))
      (defdb korma-db current-db-spec))))

(defn korma-init
  "Initializes korma database"
  []
  @korma-init-done)
