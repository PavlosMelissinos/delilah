(ns philistine.routes
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http.ring-middlewares :as rm]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [clj-http.client :as http]))

(defn routes []
    (route/expand-routes
      #{["/" :get [interceptors/home]
         :route-name :home]}))
