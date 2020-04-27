(ns delilah.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]

            [delilah.dei.core :as dei]))

(defn do-task []
  (let [ctx (-> (io/resource "config.edn")
                slurp
                (edn/read-string))]
    (dei/do-task ctx)))

(defn -main
  [& args]
  (do-task)
  (System/exit 0))
