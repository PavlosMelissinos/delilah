(ns philistine.system
  (:require [clojure.java.io :as io]

            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [me.raynes.fs    :as fs]))


(defmethod ig/init-key :delilah/services
  [_ config]
  config)

(defmethod ig/halt-key! :delilah/services
  [_ _])

(defmethod ig/init-key :delilah/cache-dir
  [_ config]
  config)

(defmethod ig/halt-key! :delilah/cache-dir
  [_ _])

(defn normalize-path [path]
  (-> path fs/expand-home fs/absolute))

(defn enrich-provider-cfg [{:keys [provider] :as provider-cfg} default-cache-dir secrets]
  (let [cache-dir (format "%s/%s" default-cache-dir (name provider))]
    (merge provider-cfg
           {:cache-dir    cache-dir
            :download-dir (str cache-dir "/downloads")}
           (get secrets provider))))

(defn load-config [filename]
  (let [{:delilah/keys [services cache-dir] :as ctx} (-> (io/resource filename) slurp ig/read-string)
        secrets                                      (-> (io/resource "secrets.edn") slurp ig/read-string)
        cache-dir                                    (normalize-path cache-dir)]
    (assoc ctx :delilah/services (map #(enrich-provider-cfg % cache-dir secrets) services))))
