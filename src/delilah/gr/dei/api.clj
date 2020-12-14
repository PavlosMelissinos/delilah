(ns delilah.gr.dei.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.reader.edn :as edn]

            [clj-http.client :as http]
            [me.raynes.fs :as fs]

            [delilah.gr.dei.scraper :as scraper]
            [delilah.gr.dei :as dei]))

(def save-pdf! scraper/save-pdf!)

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
         data     (scraper/scrape cfg)]
     (if save-files?
       (doseq [bill (:bills data)]
         (scraper/save-pdf! bill))
       data))))
(s/fdef extract
  :args (s/cat :cfg ::dei/cfg))

(comment
  (def cfg (-> "~/.config/delilah/secrets.edn"
               fs/expand-home
               slurp
               (edn/read-string)))

  (def data (extract cfg)))
