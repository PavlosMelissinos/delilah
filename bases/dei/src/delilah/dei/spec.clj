(ns delilah.dei.spec
  (:require [clojure.spec.alpha :as s]
            [delilah :as d]
            [delilah.dei.pdf :as pdf]))

;; Config specs
(s/def ::user string?)
(s/def ::pass string?)
(s/def ::save-files? boolean?)

(s/def ::pdf/areas (s/coll-of ::pdf/area))

(s/def ::cfg (s/keys
              :req    [:delilah.gr.dei/user
                       :delilah.gr.dei/pass]
              :opt-un [::save-files?
                       ::d/cache-dir]))
