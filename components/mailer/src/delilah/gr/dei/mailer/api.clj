(ns delilah.gr.dei.mailer.api
  (:require [clojure-mail.core :as mail]
            [clojure-mail.message :as msg]
            [taoensso.timbre :as log]

            [delilah.gr.dei.mailer.parser :as parser]))

(defn do-task [{:keys [imap user pass folder] :as mailer}]
  (log/info (str "Retrieving dei bill information from server "
                 imap
                 ", for user " user
                 (when folder
                   ", in folder " folder)
                 "."))
  (let [all-messages (let [store (mail/store imap user pass)]
                       (mail/all-messages store folder))
        filter-fn    (-> mailer :filter-fn eval)
        power-bills  (->> (map msg/read-message all-messages)
                          (filter filter-fn))]
    (log/info "Parsing emails...")
    (map parser/parse power-bills)))

(comment
  (def cfg
    {:folder        "[Gmail]/All Mail"
     :imap          "imap.gmail.com"
     :filter-fn     #(str/includes? (:subject %) "ΔΕΗ")})
  ;; get email content
  (require '[clojure.tools.reader.edn :as edn])
  (defn load-settings []
    (-> "/home/thirstytm/.config/delilah/secrets.edn"
        slurp
        edn/read-string))

  (def mailer (:delilah/mailer (load-settings)))

  (do-task mailer))
