(ns com.sixsq.slipstream.ssclj.format.xml-format-test
  (require
    [com.sixsq.slipstream.ssclj.format.xml-format :refer :all]
    [expectations :refer :all]))

(expect ["http://example.org/1" "object"] (split-resource-uri "http://example.org/1/object"))
(expect nil (split-resource-uri nil))
(expect nil (split-resource-uri ""))
(expect nil (split-resource-uri "invalid-resource-uri"))

