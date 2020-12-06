(ns delilah
  (:require [clojure.spec.alpha :as s]
            [etaoin.api :as api]))

(s/def ::driver #(api/driver? % "firefox"))
