(ns com.sixsq.slipstream.tools.cli.utils
  (:require
    [clojure.string :as str]
    [taoensso.timbre :as log]))


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


(defn error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))
