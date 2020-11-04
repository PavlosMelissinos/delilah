(ns delilah.api
  (:require [delilah.gr.dei.core :as dei]
            [delilah.gr.deddie.core :as deddie))

(defn parse [{:keys [provider] :as ctx}]
  (cond
    (= provider :dei) (dei/do-task ctx)
    (= provider :deddie) (deddie/do-task ctx)
    :else             (throw
                        (ex-info "Unrecognized provider" {:provider provider}))))


(comment
  (let [ctx {:provider  :dei
             :cache-dir "/path/to/delilah/cache/dei"
             :user      ""
             :pass      ""}]
    (parse ctx)))
