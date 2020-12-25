(ns delilah
  (:require [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]))

(s/def ::cache-dir fs/directory?)
(s/def :delilah.driver/type #{:firefox})
(s/def :delilah.driver/headless boolean?)

(s/def ::driver
  (s/keys :req-un [:delilah.driver/type]
          :opt-un [:delilah.driver/headless]))
