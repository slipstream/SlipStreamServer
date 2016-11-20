(ns sixsq.slipstream.server.ring-container-test
  (:require
   [clojure.test :refer :all]
   [sixsq.slipstream.server.ring-container :as t]))

(deftest check-dyn-resolve
  (let [dyn-resolve @#'t/dyn-resolve]
    (are [input] (dyn-resolve input)
         'environ.core/env
         "environ.core/env"
         'aleph.http/start-server
         "aleph.http/start-server")
    (is (thrown? Exception (dyn-resolve 'unknown/var)))
    (is (thrown? Exception (dyn-resolve "unknown/var")))))

(deftest check-parse-port
  (let [parse-port @#'t/parse-port]
    (are [expected input] (= expected (parse-port input))
         t/default-port nil
         t/default-port 10.2
         t/default-port "invalid"
         t/default-port 65537
         t/default-port -1
         10 10
         10 "10")))

