(ns delilah.api
  (:require [delilah.gr.dei.api :as dei]
            [delilah.gr.deddie.api :as deddie]))

(defn parse [{:keys [provider] :as ctx}]
  (cond
    (= provider :dei)    (dei/extract ctx)
    (= provider :deddie) (deddie/outages ctx)
    :else                (throw
                          (ex-info "Unrecognized provider" {:provider provider}))))


(comment
  (require '[me.raynes/fs :as fs]
           '[clojure.tools.reader.edn :as edn])
  (def ctx (-> "~/.config/delilah/secrets.edn"
               fs/expand-home
               slurp
               (edn/read-string)))

  (parse (assoc ctx :provider :dei)))
