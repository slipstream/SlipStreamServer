(ns sixsq.slipstream.pricing.service.server
  (:require
    [compojure.core :refer :all]
    [compojure.route :as route]
    [clojure.data.json :as json]
    [org.httpkit.client :as client]
    [clojure.tools.logging :as log]

    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [clojure.xml :as xml]
    [ring.middleware.json :refer [wrap-json-params]]
    [clojure.walk :refer [keywordize-keys]]
    [aleph.http :as http]
    [sixsq.slipstream.pricing.lib.pricing :refer :all]
    [sixsq.slipstream.placement.core :as pc])

  (:import (java.util.concurrent Executors TimeUnit)))

;;UTILS
(def baseparams
  {:basic-auth ["olivier" "hepiaO7i%"] :insecure? true})

(defn get-req
  [url params]
  (:body @(client/get url params)))


(defn parse
  [s]
  (xml/parse
    (java.io.ByteArrayInputStream. (.getBytes s))))

;;service catalog

(declare servicecatalog)
(defn get-service-catalog
  []
  (println "Service catalog updated !")
  (def servicecatalog (json/read-str
                        (get-req "https://129.194.184.194/api/service-info" baseparams) :key-fn keyword)))

(defn service-info-for-cloudname
  [cloudname]
  (first (filter (fn [{{n :href} :connector}] (= n cloudname)) (:service-info servicecatalog))))


(defn get-service-catalog-entity
  [cloudname resourcetype resourcename]
  (let [{{l (keyword resourcetype)} :pricing} (service-info-for-cloudname cloudname)
        [entity] (filter #(= resourcename (:name %)) l)]
    entity))



(defn start-service-catalog-update
  [seconds]
  (.scheduleAtFixedRate (Executors/newScheduledThreadPool 1)
                        #(get-service-catalog) 0 seconds TimeUnit/SECONDS))



;;RUNPRICE
(defn get-price-for-run
  [params]
  "Yep. I do nothing !")


;;COSTS
(defn get-cost
  [{:keys [cloudname resourcetype resourcename quantity]}]
  (let [entity (get-service-catalog-entity cloudname resourcetype resourcename)]
    (str (compute-cost entity quantity))))


;;TEMP RUN INFOS - FOR DEMO
;;Return a module from a module url
(defn gm
  [moduleurl]
  (parse (get-req (str "https://129.194.184.194/" moduleurl) baseparams)))


;;Return the instance type of a module
(defn getinstancetype
  ([module provider]
   (getinstancetype module provider "instance.type"))

  ([module provider type]
   (->> (:content module)
        (filter #(= (:tag %) :parameters))
        first
        :content
        (filter #(= (first (:content (first (:content %)))) (str provider "." type)))
        first
        :content
        (filter #(= (:tag %) :parameter))
        first
        :content
        (filter #(= (:tag %) :value))
        first
        :content
        first)))




;;Recursively find the base image of a module
(defn getvm
  [module provider]
  (def moduleurl (:moduleReferenceUri (:attrs module)))
  (if moduleurl (getvm (gm moduleurl) provider) (getinstancetype module provider)))


(defn- put-place-and-rank
  [request]
  (let [prs-req (clojure.walk/keywordize-keys (:params request))
        _ (log/debug "PRS request: " prs-req)
        prs-resp (pc/place-and-rank prs-req)
        _ (log/debug "PRS response: " prs-resp)]
    {:status 200
     :body   (json/write-str prs-resp)}))

(defn cost-json
  [request]
  (println "request")
  (clojure.pprint/pprint (into (sorted-map) request))
  (println "json-params " (:json-params request))
  (try
    (get-cost (keywordize-keys (:json-params request)))
    (catch Exception e
      (println (.getMessage e))
      {:status 500
       :body   (str "Problem with the parameters: " (.getMessage e))})))

(defroutes app-routes

           (wrap-json-params
             (PUT "/filter-rank" request (put-place-and-rank request)))

           ;;Route to compute the price of a running slipstream run
           ;;Does nothing for now
           (GET "/price/run" [& params] (get-price-for-run params))
           ;;Example request :
           ;;http://localhost:3000/resourcetype?module=module/hepiacloudtests/WebServer&cloudname=exoscale-ch-gva
           (GET "/resourcetype" [& params] (getvm (gm (:module params)) (:cloudname params)))


           ;;Example request data :
           ;;{"cloudname" : "exoscale-ch-gva", "resourcetype" : "vm", "resourcename" : "Micro", "quantity" : [{"timeCode" : "HUR", "sample" : 1, "values" : [1]}]}
           (wrap-json-params
             (POST "/price/entity" request (cost-json request)))

           ;CORS for json
           (OPTIONS "/price/entity" request (-> {}
                                                (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
                                                (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type")
                                                (assoc-in [:headers "Access-Control-Allow-Methods"] "POST")))


           (route/not-found {:status 404 :body "Not found"}))


;; MIDDLEWARE TO ALLOW CROSS ORIGIN FOR DEMO
(defn wrap-cross-origin [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Access-Control-Allow-Origin"] "*"))))

(def app
  (-> app-routes
      (wrap-defaults (assoc site-defaults :security (assoc (:security site-defaults) :anti-forgery false)))
      (wrap-cross-origin)))


(defn start
  [port]
  (start-service-catalog-update 600)
  (let [s (http/start-server app {:port port})]
    (fn [] (.close s))))

(defn stop
  "Stops the application server by calling the function that was
   created when the application server was started."
  [stop-fn]
  (try
    (and stop-fn (stop-fn))
    (catch Exception e (println "Erm,...,."))))
