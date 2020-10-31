(ns delilah.api
  (:require [delilah.dei.core :as dei]))

(defn parse [{:keys [provider] :as ctx}]
  (cond
    (= provider :dei) (dei/do-task ctx)
    (= provider :deddie) (deddie.gr/do-task ctx)
    :else             (throw
                        (ex-info "Unrecognized provider" {:provider provider}))))


(comment
  (let [ctx {:provider  :dei
             :cache-dir "/path/to/delilah/cache/dei"
             :user      ""
             :pass      ""}]
    (parse ctx)))
