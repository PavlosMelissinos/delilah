(ns delilah.dei.api
  (:require [delilah.dei.core :as dei]))

(defn save-pdf! [ctx filepath]
  (dei/save-pdf! ctx filepath))

(defn latest-bill [ctx]
  (dei/latest-bill ctx))

(defn extract [cfg]
  (dei/extract cfg))

(comment
  (require '[me.raynes.fs :as fs]
           '[clojure.tools.reader.edn :as edn])
  (def cfg (-> "~/.config/delilah/secrets.edn"
               fs/expand-home
               slurp
               (edn/read-string)))

  (def data (extract cfg)))
