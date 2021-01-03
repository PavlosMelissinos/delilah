(ns delilah.gr.dei.mailer
  (:require [clojure.spec.alpha :as s]))

(s/def ::enrich? boolean?)
(s/def ::imap string?)
(s/def ::user string?)
(s/def ::pass string?)
(s/def ::folder string?)
(s/def ::filter-fn coll?)


(s/def ::cfg
  (s/keys :req [::user ::pass]
          :opt [::enrich? ::imap ::filter-fn ::folder]))
