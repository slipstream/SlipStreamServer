(ns com.sixsq.slipstream.ssclj.app.SscljTestServer
  (:gen-class
    :methods [^{:static true} [start [] void]
              ^{:static true} [stop [] void]
              ^{:static true} [refresh [] void]]))


(def ^:const server-ns 'com.sixsq.slipstream.ssclj.app.test-server)


(defn -start
  []
  (require server-ns)
  (let [start-fn (-> server-ns
                     find-ns
                     (ns-resolve 'start))]
    (start-fn)))


(defn -stop
  []
  (require server-ns)
  (let [stop-fn (-> server-ns
                    find-ns
                    (ns-resolve 'stop))]
    (stop-fn)))


(defn -refresh
  []
  (require server-ns)
  (let [stop-fn (-> server-ns
                    find-ns
                    (ns-resolve 'refresh-es-indices))]
    (stop-fn)))
