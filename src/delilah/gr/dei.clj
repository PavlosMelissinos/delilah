(ns delilah.gr.dei
  (:require [clojure.spec.alpha :as s]
            [delilah :as d]))

(s/def ::user string?)
(s/def ::pass string?)
(s/def ::save-files? boolean?)

(s/def ::cfg (s/keys
              :req    [:delilah.gr.dei/user
                       :delilah.gr.dei/pass]
              :opt-un [::save-files?
                       ::d/cache-dir]))
