(ns philistine.service
  (:require [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.route.definition :refer [defroutes]]))


(defn hello-world
  [request]
  (let [name (get-in request [:params :name] "World")]
    {:status 200 :body (str "Hello, " name "!\n")}))

(defroutes routes
           [[["/"
              ["/hello" {:get hello-world}]]]])

(defn service
  ([port]
   (-> {::http/routes         routes
        ::http/resource-path  "/public"
        ::http/join?          false
        ::http/type           :jetty
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
