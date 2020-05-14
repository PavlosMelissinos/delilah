(ns philistine.main
  (:require [integrant.core :as ig]
            [io.pedestal.log :as log]

            [delilah.api :as delilah]
            [philistine.db :as db]
            [philistine.system :as system]))

(defn do-task []
  (let [{:db/keys [connection] :delilah/keys [services] :as ctx} (system/load-config "config.edn")

        active-services (filter :active services)
        db (db/init connection)]
    (def ctx ctx)
    (def db db)
    (doseq [provider-cfg active-services]
      (->> (delilah/parse provider-cfg)
           (db/store-data! db provider-cfg)))))

(defn -main [& args]
  (let [config (system/load-config "config.edn")]
    (log/info :msg (str "Starting on port " (get-in config [:http/service :port])))
    (ig/init config)))
