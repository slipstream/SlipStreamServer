(ns com.sixsq.slipstream.tools.cli.users-identifiers
  (:require
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [com.sixsq.slipstream.db.filter.parser :as parser]
    [com.sixsq.slipstream.db.impl :as db]
    [com.sixsq.slipstream.db.loader :as db-loader]
    [com.sixsq.slipstream.ssclj.resources.user-identifier]
    [com.sixsq.slipstream.ssclj.resources.user.user-identifier-utils :as uiu]
    [taoensso.timbre :as log])
  (:gen-class))

(def default-db-binding-ns "com.sixsq.slipstream.db.es.loader")

(defn init-db-client
  [binding-ns]
  (db-loader/load-and-set-persistent-db-binding binding-ns))




(def cli-options
  [
   ["-u" "--user USER" "Create user identifier for provided user id."]
   ["-i" "--instance INSTANCE" "Will be used as prefix in identifier, eg SixSq"]
   ["-e" "--external EXTERNAL-LOGIN" "The unmangled federated username"]
   ["-h" "--help"]
   [nil "--logging LEVEL" "Logging level: trace, debug, info, warn, error, fatal, or report."
    :id :level
    :default :info
    :parse-fn #(-> % str/lower-case keyword)]])

(defn exit
  ([]
   (exit 0 nil))
  ([status]
   (exit status nil))
  ([status msg]
   (when msg
     (if-not (zero? status)
       (log/error msg)
       (log/info msg)))
   (System/exit status)))

(defn success
  ([]
   (exit))
  ([msg]
   (exit 0 msg)))

(defn failure
  [& msg]
  (exit 1 (str/join msg)))

(defn add-identifier
  [username instance external-login]


  (log/info (str "Creating user identifier for user/" username " with " instance":"external-login) )
  (uiu/add-user-identifier! username :oidc external-login instance)

  )


(defn -main [& args]
  (let [{:keys [options summary]} (cli/parse-opts args cli-options)
        required-params? (and (:user options) (:instance options) (:external options))
        _ (init-db-client default-db-binding-ns)]

    (log/set-level! (:level options :info))

    (cond
      (:help options) (clojure.pprint/pprint (success summary))
      required-params? (add-identifier (:user options) (:instance options) (:external options))
      :else (clojure.pprint/pprint (failure summary)))
    (clojure.pprint/pprint "DONE")))

