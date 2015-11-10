(ns com.sixsq.slipstream.ssclj.app.reports
  (:require
    [clojure.tools.logging :as log]
    [clojure.java.io :as io]
    [compojure.core :refer [PUT]]
    [compojure.route :as route]
    [com.sixsq.slipstream.ssclj.resources.common.utils :as u])
  (:import [java.io IOException]))

(def ^:const report-root "/var/tmp/slipstream/reports")

(defn copy-stream-to-file [body file]
  (if (and body file)
    (try
      (with-open [input-stream body]
        (io/copy input-stream file)
        (log/info "uploaded reports to" file))
      (catch IOException ex
        (u/map-response "unable to upload reports" 500 file)
        (log/error "unable to upload reports" file)))
    (do
      (u/map-response "invalid upload reports request" 400 file)
      (log/error "invalid upload reports request" file))))

(def download-route
     (route/files "/reports"
                   :root report-root
                   :mime-types {"tgz", "application/x-gzip"}))

(def upload-route
     (PUT "/reports/*" {{:keys [*]} :params :as request}
          (copy-stream-to-file (:body request) (str report-root :*))))
