(ns delilah.api
  (:require [delilah.dei.core :as dei]))

(defn parse [{:keys [provider] :as ctx}]
  (cond
    (= provider "dei") (dei/do-task ctx)
    :else              (throw
                         (ex-info "Unrecognized provider" {:provider provider}))))


(comment
  (do
    (def ctx {:provider "dei"
              :cache-dir "/path/to/delilah/cache/dei"
              :user     ""
              :pass     ""})
    (parse ctx)))
