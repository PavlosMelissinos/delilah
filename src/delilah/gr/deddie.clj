(ns delilah.gr.deddie
  (:require [clojure.spec.alpha :as s]))

(s/def :deddie.prefecture/name string?)
(s/def :deddie.prefecture/selected boolean?)
(s/def :deddie.prefecture/id integer?)

(s/def :deddie/prefecture
  (s/keys :req [:deddie.prefecture/name
                :deddie.prefecture/id]
          :opt [:deddie.prefecture/selected]))

(s/def :deddie.municipality/name string?)
(s/def :deddie.municipality/selected boolean?)
(s/def :deddie.municipality/id integer?)

(s/def :deddie/municipality
  (s/keys :req [:deddie.municipality/name
                :deddie.municipality/id
                :deddie.prefecture/name
                :deddie.prefecture/id]
          :opt [:deddie.municipality/selected
                :deddie.prefecture/selected]))
