(ns philistine.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [delilah.api :as delilah]
            [philistine.db :as db]))

(defn enrich-provider-cfg [{:keys [provider] :as provider-cfg} default-cache-dir]
  (assoc provider-cfg :cache-dir (format "%s/%s" default-cache-dir provider)))

(defn load-config []
  (let [{:delilah/keys [services] :keys [cache-dir] :as ctx}
        (-> (io/resource "config.edn")
            slurp
            edn/read-string)]
    (assoc ctx :delilah/services (map #(enrich-provider-cfg % cache-dir) services))))

(defn do-task []
  (let [{:db/keys [connection] :delilah/keys [services] :as ctx} (load-config)

        active-services (filter :active services)
        db (db/init connection)]
    (def ctx ctx)
    (def db db)
    (doseq [provider-cfg active-services]
      (def provider-cfg provider-cfg)
      (def res (delilah/parse provider-cfg))
      (db/store-data! db provider-cfg res))))

(defn -main
  [& args]
  (do-task)
  (System/exit 0))
