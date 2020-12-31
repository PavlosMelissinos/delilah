(ns delilah.gr.dei.mailer.api
  (:require [clojure-mail.core :as mail]
            [clojure-mail.message :as msg]
            [taoensso.timbre :as log]

            [delilah.gr.dei.mailer.parser :as parser]))

(defn do-task [{:delilah.gr.dei.mailer/keys [imap user pass folder filter-fn]
                :as cfg}]
  (log/info (str "Retrieving dei bill information from server "
                 imap
                 ", for user " user
                 (when folder
                   ", in folder " folder)
                 "."))
  (let [all-messages (let [store (mail/store imap user pass)]
                       (mail/all-messages store folder))
        filter-fn    (eval filter-fn)
        power-bills  (->> (map msg/read-message all-messages)
                          (filter filter-fn))]
    (log/info "Parsing emails...")
    (map parser/parse power-bills)))

(comment
  ;; get email content
  (require '[clojure.tools.reader.edn :as edn])
  (def settings
    (-> "/home/thirstytm/.config/delilah/secrets.edn"
        slurp
        edn/read-string))

  (do-task settings))
