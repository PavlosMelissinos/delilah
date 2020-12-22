(ns delilah
  (:require [clojure.spec.alpha :as s]))

(s/def ::cache-dir string?)
(s/def ::provider keyword?)
