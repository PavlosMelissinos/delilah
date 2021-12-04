(ns delilah.dei.spec
  (:require [clojure.spec.alpha :as s]))

;; Config specs
(s/def :delilah/cache-dir #(or (string? %) (fs/directory? %)))
(s/def :delilah/provider keyword?)

(s/def :delilah.dei/user string?)
(s/def :delilah.dei/pass string?)
(s/def :delilah.dei/save-files? boolean?)

(s/def ::pdf/areas (s/coll-of ::pdf/area))

(s/def :delilah.dei/cfg
  (s/keys :req    [:delilah.dei/user
                   :delilah.dei/pass]
          :opt-un [:delilah.dei/save-files?
                   :delilah/cache-dir]))
