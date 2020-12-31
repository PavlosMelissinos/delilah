(ns delilah
  (:require [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]))

(s/def ::cache-dir #(or (string? %) (fs/directory? %)))
(s/def ::provider keyword?)
