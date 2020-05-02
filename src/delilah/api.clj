(ns delilah.api
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [delilah.dei.core :as dei]))

(defn parse [{:keys [provider] :as ctx}]
  (cond
    (= provider "dei") (dei/do-task ctx)
    :else              (throw
                         (ex-info "Unrecognized provider" {:provider provider}))))


(comment
  (let [ctx (->> (io/resource "config.edn")
                 slurp
                 (edn/read-string)
                 (assoc :provider "dei"))]
    (parse ctx)))
