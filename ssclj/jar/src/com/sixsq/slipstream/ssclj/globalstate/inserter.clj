(ns com.sixsq.slipstream.ssclj.globalstate.inserter
  (:gen-class)
  )

(def ^:const inserter-ns 'com.sixsq.slipstream.ssclj.globalstate.inserter-impl)

(defn -main
  "-main function is dynamically loaded from namespace com.sixsq.slipstream.ssclj.globalstate.inserter-impl,
  so that dependencies are compiled at runtime."
  [& args]
  (require inserter-ns)
  (let [main-fn (-> inserter-ns
                    find-ns
                    (ns-resolve '-main))]
    (apply main-fn args)))