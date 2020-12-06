(ns delilah.gr.deddie
  (:require [clojure.spec.alpha :as s]))

(def prefectures
  #{""
    "ΑΙΤΩΛΟΑΚΑΡΝΑΝΙΑΣ"
    "ΑΡΓΟΛΙΔΑΣ"
    "ΑΡΚΑΔΙΑΣ"
    "ΑΡΤΑΣ"
    "ΑΤΤΙΚΗΣ"
    "ΑΧΑΙΑΣ"
    "ΒΟΙΩΤΙΑΣ"
    "ΓΡΕΒΕΝΩΝ"
    "ΔΡΑΜΑΣ"
    "ΔΩΔΕΚΑΝΗΣΟΥ"
    "ΕΒΡΟΥ"
    "ΕΥΒΟΙΑΣ"
    "ΕΥΡΥΤΑΝΙΑΣ"
    "ΖΑΚΥΝΘΟΥ"
    "ΗΛΕΙΑΣ"
    "ΗΜΑΘΙΑΣ"
    "ΗΡΑΚΛΕΙΟΥ"
    "ΘΕΣΠΡΩΤΙΑ"
    "ΘΕΣΣΑΛΟΝΙΚΗΣ"
    "ΙΩΑΝΝΙΝΩΝ"
    "ΚΑΒΑΛΑΣ"
    "ΚΑΡΔΙΤΣΑΣ"
    "ΚΑΣΤΟΡΙΑΣ"
    "ΚΕΡΚΥΡΑΣ"
    "ΚΕΦΑΛΛΟΝΙΑΣ"
    "ΚΙΛΚΙΣ"
    "ΚΟΖΑΝΗΣ"
    "ΚΟΡΙΝΘΙΑΣ"
    "ΚΥΚΛΑΔΩΝ"
    "ΛΑΚΩΝΙΑΣ"
    "ΛΑΡΙΣΑΣ"
    "ΛΑΣIΘΙΟΥ"
    "ΛΕΣΒΟΥ"
    "ΛΕΥΚΑΔΑΣ"
    "ΜΑΓΝΗΣΙΑΣ"
    "ΜΕΣΣΗΝΙΑΣ"
    "ΞΑΝΘΗΣ"
    "ΠΕΛΛΗΣ"
    "ΠΙΕΡΙΑΣ"
    "ΠΡΕΒΕΖΗΣ"
    "ΡΕΘΥΜΝΟΥ"
    "ΡΟΔΟΠΗΣ"
    "ΣΑΜΟΥ"
    "ΣΕΡΡΩΝ"
    "ΤΡΙΚΑΛΩΝ"
    "ΦΘΙΩΤΙΔΑΣ"
    "ΦΛΩΡΙΝΑΣ"
    "ΦΩΚΙΔΑΣ"
    "ΧΑΛΚΙΔΙΚΗΣ"
    "ΧΑΝΙΩΝ"
    "ΧΙΟΥ"})

(s/def :deddie.prefecture/name (s/and string?
                                      prefectures))
(s/def :deddie.prefecture/selected boolean?)
(s/def :deddie.prefecture/id string?)

(s/def :deddie/prefecture
  (s/keys :req [:deddie.prefecture/name
                :deddie.prefecture/id]
          :opt [:deddie.prefecture/selected]))

(s/def :deddie.municipality/name string?)
(s/def :deddie.municipality/selected boolean?)
(s/def :deddie.municipality/id string?)

(s/def :deddie/municipality
  (s/keys :req [:deddie.municipality/name
                :deddie.municipality/id
                :deddie.prefecture/name
                :deddie.prefecture/id]
          :opt [:deddie.municipality/selected
                :deddie.prefecture/selected]))
