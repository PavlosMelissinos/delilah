(ns delilah.gr.dei.api
  (:require [delilah.gr.dei.core :as dei]))

(def save-pdf! dei/save-pdf!)
(def latest-bill dei/latest-bill)
(def extract dei/extract)

(comment
  (require '(me.raynes.fs :as fs)
           '(clojure.tools.reader.edn :as edn))
  (def cfg (-> "~/.config/delilah/secrets.edn"
               fs/expand-home
               slurp
               (edn/read-string)))

  (def data (extract cfg)))
