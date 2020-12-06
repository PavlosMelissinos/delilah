(ns delilah.api
  (:require [delilah.gr.dei.core :as dei]
            [delilah.gr.deddie.api :as deddie]))

(defn parse [{:keys [provider] :as ctx}]
  (cond
    (= provider :dei) (dei/do-task ctx)
    (= provider :deddie) (deddie/outages ctx)
    :else             (throw
                        (ex-info "Unrecognized provider" {:provider provider}))))


(comment
  (let [ctx {:provider  :dei
             :cache-dir "/path/to/delilah/cache/dei"
             :user      ""
             :pass      ""}]
    (parse ctx)))
