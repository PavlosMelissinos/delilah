(ns philistine.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [next.jdbc :as jdbc]
            [next.jdbc.connection :as connection]
            ;[jdbc.pool.c3p0 :as pool]

            [delilah.api :as delilah])
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource PooledDataSource)))


(defn app-init []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))

(defn do-task []
  (let [ctx             (-> (io/resource "config.edn")
                            slurp
                            (edn/read-string))
        active-services (->> ctx
                             :delilah.services
                             (filter :active))]
    #_(app-init)
    (doseq [{:keys [provider] :as provider-cfg} active-services]
      (-> provider-cfg
          (assoc :cache-dir (format "%s/%s" (:cache-dir ctx) provider))
          delilah/parse))))

(defn -main
  [& args]
  (do-task)
  (System/exit 0))

(comment
  (do
    (require '[jdbc.pool.c3p0 :as pool])
    (def conn (pool/make-datasource-spec {:connection-uri    ""
                                          :classname         "org.postgresql.Driver"
                                          :user              ""
                                          :password          ""
                                          :initial-pool-size 3
                                          :min-pool-size     3
                                          :max-pool-size     15}))))
