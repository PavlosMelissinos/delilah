(ns philistine.service
  (:require [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            #_[io.pedestal.http.route.definition :refer [defroutes]]
            [philistine.routes :as routes]))


(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body (str "Hello, " name "!\n")}))

(def routes
  (route/expand-routes
    #{["/" :get [interceptors/home]
       :route-name :home ]}))

(defn service
  ([port]
   (-> {::http/routes         routes
        ::http/resource-path  "/public"
        ::http/join?          false
        ::http/type           :jetty
        ::http/host           "0.0.0.0"
        ::http/port           port}
       (http/default-interceptors)
       (http/dev-interceptors))))

(defmethod ig/init-key :http/service
  [_ {:keys [port]}]
  (-> (service port)
      http/create-server
      http/start))

(defmethod ig/halt-key! :http/service [_ server]
  (http/stop server))
