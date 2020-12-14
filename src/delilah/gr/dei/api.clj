(ns delilah.gr.dei.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.reader.edn :as edn]

            [clj-http.client :as http]

            [delilah.gr.dei.core :as dei]
            [delilah.gr.dei :as ds]))

(def save-pdf! dei/save-pdf!)

(defn latest-bill [{:keys [bills] :as data}]
  (->> bills (sort-by :bill-date) reverse first))

(defn extract
  ([{:keys [save-files?] :as cfg}]
   (let [cfg-base (-> (io/resource "config.edn")
                      slurp
                      (edn/read-string))
         cfg      (-> (merge cfg-base cfg)
                      (update-in [:driver :path-driver] fs/expand-home)
                      (update :delilah/cache-dir fs/expand-home))
         data     (dei/scrape cfg)]
     (if save-files?
       (doseq [bill (:bills data)]
         (dei/save-pdf! bill))
       data))))
(s/fdef extract
  :args (s/cat :cfg ::ds/cfg))

(comment
  (require '(me.raynes.fs :as fs)
           '(clojure.tools.reader.edn :as edn))
  (def cfg (-> "~/.config/delilah/secrets.edn"
               fs/expand-home
               slurp
               (edn/read-string)))

  (def data (extract cfg)))
