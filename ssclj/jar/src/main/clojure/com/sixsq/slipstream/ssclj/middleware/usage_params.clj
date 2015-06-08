(ns com.sixsq.slipstream.ssclj.middleware.usage-params
  (require [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn enrich-by-extract-single
  [req name]
  (assoc req (keyword name) (get-in req [:params name])))

(defn enrich-by-extract
  [req & names]
  (reduce enrich-by-extract-single req names))

(defn- process-from-to
  [req]
  (-> (enrich-by-extract req "from" "duration")
      ; du/show
      ))

(defn wrap-usage-params
  "Middleware that processes Usage parameters from the :params map
   in the request.  Because this uses the :params map, the wrap-params
   middleware **must** be run prior to this wrapper in the ring
   processing chain."
  [handler]
  (fn [req]
    (->> req
         (process-from-to)
         (handler))))