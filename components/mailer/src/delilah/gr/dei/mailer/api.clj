(ns delilah.gr.dei.mailer.api
  (:require [clojure.spec.alpha :as s]
            [clojure-mail.core :as mail]
            [clojure-mail.folder :as folder]
            [clojure-mail.message :as msg]
            [taoensso.timbre :as log]

            [delilah.gr.dei.mailer.parser :as parser]))

(defn do-task [{:delilah.gr.dei.mailer/keys [imap user pass folder search]
                :as cfg}]
  (log/info (str "Retrieving dei bill information from server " imap
                 ", for user " user
                 (when search (str ", containing the text '" search "'"))
                 (when folder (str ", in folder " folder))
                 "."))
  (let [power-bills (as-> (mail/store imap user pass) x
                      (mail/open-folder x folder :readonly)
                      (folder/search x search)
                      (map msg/read-message x))]
    (map parser/parse power-bills)))
(s/fdef do-task
  :args (s/cat :cfg (s/keys
                     :req [:delilah.gr.dei.mailer/imap
                           :delilah.gr.dei.mailer/user
                           :delilah.gr.dei.mailer/pass
                           :delilah.gr.dei.mailer/folder
                           :delilah.gr.dei.mailer/search])))

(comment
  ;; get email content
  (require '[clojure.tools.reader.edn :as edn])
  (require '[clojure-mail.folder :as folder])
  (def settings
    (-> "/home/thirstytm/.config/delilah/secrets.edn"
        slurp
        edn/read-string))

  (do-task settings)

  (let [{:delilah.gr.dei.mailer/keys [imap user pass folder filter-fn]
         :as cfg} settings]
    (def store (mail/store imap user pass))
    )
  (def folder (mail/open-folder store "[Gmail]/All Mail" :readonly))
  (->> (folder/search folder "ΔΕΗ e-bill")
       (map msg/read-message)
       (map :subject))

  (->> (folder/search folder "ΔΕΗ e-bill")
       (map msg/read-message)
       (map #(select-keys % [:subject :date-sent])))
  (mail/search-inbox))
