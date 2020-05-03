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
  (let [{:db/keys [connection] :delilah/keys [services]} (load-config)

        active-services (filter :active services)]
    (db/init connection)
    (doseq [provider-cfg active-services]
      (delilah/parse provider-cfg))))

(defn -main
  [& args]
  (do-task)
  (System/exit 0))
