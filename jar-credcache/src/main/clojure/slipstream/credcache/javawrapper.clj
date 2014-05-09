(ns slipstream.credcache.javawrapper
  (:require
    [clojure.walk :as walk]
    [slipstream.credcache.control :as ctl]
    [slipstream.credcache.credential :as cred]
    [slipstream.credcache.credential.myproxy-voms :as mpv])
  (:gen-class
    :name slipstream.credcache.JavaWrapper
    :methods [#^{:static true} [start [java.util.Map java.util.Map] void]
              #^{:static true} [stop [] void]
              #^{:static true} [create [java.util.Map] String]
              #^{:static true} [retrieve [String] java.io.File]]))

(defn -start
  [couchbase smtp]
  (let [cb-params (walk/keywordize-keys couchbase)
        smtp-params (walk/keywordize-keys smtp)]
    (ctl/start! cb-params smtp-params)))

(defn -stop
  []
  (ctl/stop!))

(defn -create
  [template]
  (let [template (walk/keywordize-keys template)]
    (cred/create template)))

(defn -retrieve
  [id]
  (mpv/proxy->tfile (cred/retrieve id)))


