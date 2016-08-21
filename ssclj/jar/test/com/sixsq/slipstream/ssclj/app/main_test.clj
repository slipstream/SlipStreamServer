(ns com.sixsq.slipstream.ssclj.app.main-test
  (:require
    [expectations :refer :all]
    [com.sixsq.slipstream.ssclj.app.main :refer :all]))

(expect nil (parse-port nil))
(expect nil (parse-port (System/getProperties)))
(expect nil (parse-port "badport"))
(expect nil (parse-port "-1"))
(expect nil (parse-port "65536"))
(expect nil (parse-port "1.3"))
(expect 1 (parse-port "1"))
(expect 65535 (parse-port "65535"))
