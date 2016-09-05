(ns com.sixsq.slipstream.ssclj.usage.summarizer
  (:gen-class))

(def ^:const summarizer-ns 'com.sixsq.slipstream.ssclj.usage.summarizer-impl)

(defn -main
  "-main function is dynamically loaded from namespace com.sixsq.slipstream.ssclj.usage.summarizer-impl,
  so that dependencies are compiled at runtime."
  [& args]
  (require summarizer-ns)
  (let [main-fn (-> summarizer-ns
                    find-ns
                    (ns-resolve '-main))]
    (apply main-fn args)))
