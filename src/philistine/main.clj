(ns philistine.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [me.raynes.fs :as fs]

            [delilah.api :as delilah]
            [philistine.db :as db]))

(defn normalize-path [path]
  (-> path fs/expand-home fs/absolute))

(defn enrich-provider-cfg [{:keys [provider] :as provider-cfg} default-cache-dir secrets]
  (let [cache-dir (format "%s/%s" default-cache-dir (name provider))]
    (merge provider-cfg
           {:cache-dir    cache-dir
            :download-dir (str cache-dir "/downloads")}
           (get secrets provider))))

(defn load-config []
  (let [{:delilah/keys [services cache-dir] :as ctx} (-> (io/resource "config.edn") slurp edn/read-string)
        secrets                                      (-> (io/resource "secrets.edn") slurp edn/read-string)
        cache-dir                                    (normalize-path cache-dir)]
    (assoc ctx :delilah/services (map #(enrich-provider-cfg % cache-dir secrets) services))))

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
