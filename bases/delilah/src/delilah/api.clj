(ns delilah.api
  (:require [clojure.spec.alpha :as s]
            [me.raynes.fs :as fs]

            [delilah.dei.api :as dei]
            [delilah.deddie.api :as deddie]))

(s/def :delilah/cache-dir fs/directory?)
(s/def :delilah.driver/type #{:firefox})
(s/def :delilah.driver/headless boolean?)

(s/def ::driver
  (s/keys :req-un [:delilah.driver/type]
          :opt-un [:delilah.driver/headless]))


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
