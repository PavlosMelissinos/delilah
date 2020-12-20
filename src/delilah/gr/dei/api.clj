(ns delilah.gr.dei.api
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.reader.edn :as edn]

            [clj-http.client :as http]
            [me.raynes.fs :as fs]

            [delilah.gr.dei.core :as dei]))

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
