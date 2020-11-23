(ns delilah.gr.deddie.parser-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clojure.tools.reader.edn :as edn]

            [java-time :as t]

            [delilah.gr.deddie.parser :as sut]))

(def dom (-> "gr/deddie/dom.edn"
             io/resource
             slurp
             edn/read-string))

(deftest test-latin-timestr
  (is (= "01:00 PM" (sut/latin-timestr "01:00 μμ")))
  (is (= "01:00 AM"
         (sut/latin-timestr "01:00 AM")
         (sut/latin-timestr "01:00 ΠΜ")
         (sut/latin-timestr "01:00 πμ")))
  (is (= "13:00" (sut/latin-timestr "13:00"))))

(deftest test-str->datetime->str
  (let [datestr  "4/5/2010 7:18:44 πμ"
        datetime (sut/str->datetime datestr)]
    (is (= datestr
           (-> (sut/datetime->str datetime "d/M/yyyy h:mm:ss a")
               (clojure.string/replace #"AM" "πμ")
               (clojure.string/replace #"PM" "μμ"))))))

(deftest test-prefecture
  (is (= {:deddie.prefecture/id "10"
          :deddie.prefecture/name "ΑΤΤΙΚΗΣ"}
         (->> dom sut/prefecture))))


(deftest test-split-area-text
  (is (= ["affected-numbers" "Ζυγά" "street" "ΕΦΕΣΟΥ" "from-street" "ΕΦΕΣΟΥ ΝΟ 26" "to-street" "ΘΡΑΚΗΣ" "from" "07:30 πμ" "to""02:30 μμ"]
         (@#'sut/split-area-text "Ζυγά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΕΦΕΣΟΥ ΝΟ 26 έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= ["affected-numbers" "Μονά" "street" "ΕΦΕΣΟΥ" "from-street" "ΚΟΥΚΛΟΥΤΖΑ" "to" "ΕΦΕΣΟΥ ΝΟ 35" "from" "07:30 πμ" "to" "02:30 μμ"]
         (@#'sut/split-area-text "Μονά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΚΟΥΚΛΟΥΤΖΑ έως: ΕΦΕΣΟΥ ΝΟ 35 από: 07:30 πμ έως: 02:30 μμ")))

  (is (= ["affected-numbers" "Μονά/Ζυγά" "street" "ΑΙΓΑΙΟΥ" "from-street" "ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ" "to" "ΑΙΓΑΙΟΥ ΝΟ 28" "from" "07:30 πμ" "to" "02:30 μμ"]
         (@#'sut/split-area-text "Μονά/Ζυγά οδός:ΑΙΓΑΙΟΥ απο κάθετο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ έως: ΑΙΓΑΙΟΥ ΝΟ 28 από: 07:30 πμ έως: 02:30 μμ")))

  (is (= ["affected-numbers" "Μονά" "street" "ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7" "to-street" "ΕΦΕΣΟΥ" "from" "07:30 πμ" "to" "02:30 μμ"]
         (@#'sut/split-area-text "Μονά      οδός:ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7 έως κάθετο: ΕΦΕΣΟΥ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= ["affected-numbers" "Μονά/Ζυγά" "street" "ΙΩΝΙΑΣ" "from-street" "ΠΕΡΓΑΜΟΥ" "to-street" "ΑΙΓΑΙΟΥ" "from" "07:30 πμ" "to" "02:30 μμ"]
         (@#'sut/split-area-text "Μονά/Ζυγά οδός:ΙΩΝΙΑΣ απο κάθετο: ΠΕΡΓΑΜΟΥ έως κάθετο: ΑΙΓΑΙΟΥ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= ["affected-numbers" "Μονά" "street" "ΒΟΥΡΝΟΒΑ" "from-street" "ΑΙΓΑΙΟΥ" "to-street" "ΘΡΑΚΗΣ" "from" "07:30 πμ" "to" "02:30 μμ"]
         (@#'sut/split-area-text "Μονά      οδός:ΒΟΥΡΝΟΒΑ απο κάθετο: ΑΙΓΑΙΟΥ έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ"))))

(deftest test-parse-area-text
  (is (= {:to-street "ΘΡΑΚΗΣ",
          :from-street "ΕΦΕΣΟΥ ΝΟ 26",
          :from (t/local-time "HH:mm" "07:30"),
          :affected-numbers "Ζυγά",
          :street "ΕΦΕΣΟΥ",
          :to (t/local-time "HH:mm" "14:30")}
         (@#'sut/affected-area "Ζυγά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΕΦΕΣΟΥ ΝΟ 26 έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= {:from-street "ΚΟΥΚΛΟΥΤΖΑ",
          :from (t/local-time "HH:mm" "07:30"),
          :affected-numbers "Μονά",
          :street "ΕΦΕΣΟΥ",
          :to (t/local-time "HH:mm" "14:30")}
         (@#'sut/affected-area "Μονά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΚΟΥΚΛΟΥΤΖΑ έως: ΕΦΕΣΟΥ ΝΟ 35 από: 07:30 πμ έως: 02:30 μμ")))

  (is (= {:from-street "ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ",
          :from (t/local-time "HH:mm" "07:30"),
          :affected-numbers "Μονά/Ζυγά",
          :street "ΑΙΓΑΙΟΥ",
          :to (t/local-time "HH:mm" "14:30")}
         (@#'sut/affected-area "Μονά/Ζυγά οδός:ΑΙΓΑΙΟΥ απο κάθετο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ έως: ΑΙΓΑΙΟΥ ΝΟ 28 από: 07:30 πμ έως: 02:30 μμ")))

  (is (= {:to-street "ΕΦΕΣΟΥ",
          :from (t/local-time "HH:mm" "07:30"),
          :affected-numbers "Μονά",
          :street "ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7",
          :to (t/local-time "HH:mm" "14:30")}
         (@#'sut/affected-area "Μονά      οδός:ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7 έως κάθετο: ΕΦΕΣΟΥ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= {:to-street "ΑΙΓΑΙΟΥ",
          :from-street "ΠΕΡΓΑΜΟΥ",
          :from (t/local-time "HH:mm" "07:30"),
          :affected-numbers "Μονά/Ζυγά",
          :street "ΙΩΝΙΑΣ",
          :to (t/local-time "HH:mm" "14:30")}
         (@#'sut/affected-area "Μονά/Ζυγά οδός:ΙΩΝΙΑΣ απο κάθετο: ΠΕΡΓΑΜΟΥ έως κάθετο: ΑΙΓΑΙΟΥ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= {:to-street "ΘΡΑΚΗΣ",
          :from-street "ΑΙΓΑΙΟΥ",
          :from (t/local-time "HH:mm" "07:30"),
          :affected-numbers "Μονά",
          :street "ΒΟΥΡΝΟΒΑ",
          :to (t/local-time "HH:mm" "14:30")}
         (@#'sut/affected-area "Μονά      οδός:ΒΟΥΡΝΟΒΑ απο κάθετο: ΑΙΓΑΙΟΥ έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ"))))

(deftest test-affected-areas
  (let [area-text "Ζυγά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΕΦΕΣΟΥ ΝΟ 26 έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΚΟΥΚΛΟΥΤΖΑ έως: ΕΦΕΣΟΥ ΝΟ 35 από: 07:30 πμ έως: 02:30 μμ\nΜονά/Ζυγά οδός:ΑΙΓΑΙΟΥ απο κάθετο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ έως: ΑΙΓΑΙΟΥ ΝΟ 28 από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7 έως κάθετο: ΕΦΕΣΟΥ από: 07:30 πμ έως: 02:30 μμ\nΜονά/Ζυγά οδός:ΙΩΝΙΑΣ απο κάθετο: ΠΕΡΓΑΜΟΥ έως κάθετο: ΑΙΓΑΙΟΥ από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΒΟΥΡΝΟΒΑ απο κάθετο: ΑΙΓΑΙΟΥ έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ"
        fmt-time-fn (fn [tm] (sut/datetime->str tm "HH:mm"))]
    (is (= [{:to-street        "ΘΡΑΚΗΣ"
             :from-street      "ΕΦΕΣΟΥ ΝΟ 26"
             :from             "07:30"
             :affected-numbers "Ζυγά"
             :street           "ΕΦΕΣΟΥ"
             :to               "14:30"}
            {:from-street      "ΚΟΥΚΛΟΥΤΖΑ"
             :from             "07:30"
             :affected-numbers "Μονά"
             :street           "ΕΦΕΣΟΥ"
             :to               "14:30"}
            {:from-street      "ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ"
             :from             "07:30"
             :affected-numbers "Μονά/Ζυγά"
             :street           "ΑΙΓΑΙΟΥ"
             :to               "14:30"}
            {:to-street        "ΕΦΕΣΟΥ"
             :from             "07:30"
             :affected-numbers "Μονά"
             :street           "ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7"
             :to               "14:30"}
            {:to-street        "ΑΙΓΑΙΟΥ"
             :from-street      "ΠΕΡΓΑΜΟΥ"
             :from             "07:30"
             :affected-numbers "Μονά/Ζυγά"
             :street           "ΙΩΝΙΑΣ"
             :to               "14:30"}
            {:to-street        "ΘΡΑΚΗΣ"
             :from-street      "ΑΙΓΑΙΟΥ"
             :from             "07:30"
             :affected-numbers "Μονά"
             :street           "ΒΟΥΡΝΟΒΑ"
             :to               "14:30"}]
           (->> area-text
                sut/affected-areas
                (map #(update % :from fmt-time-fn))
                (map #(update % :to fmt-time-fn)))))))


(deftest test-do-task
  (let [outages [{:start          "10/11/2020 8:00:00 πμ",
                  :end            "10/11/2020 1:00:00 μμ",
                  :municipality   "Ν.ΣΜΥΡΝΗΣ",
                  :affected-areas [{:to-street "ΜΕΤΡΩΝ",
                                    :from-street "ΒΟΣΠΟΡΟΥ Ν0 59",
                                    :from "08:00 πμ",
                                    :affected-numbers "Μονά/Ζυγά",
                                    :street "ΒΟΣΠΟΡΟΥ",
                                    :to "01:00 μμ"}
                                   {:to-street "ΣΩΚΙΩΝ",
                                    :from-street "ΜΕΤΡΩΝ",
                                    :from "08:00 πμ",
                                    :affected-numbers "Μονά/Ζυγά",
                                    :street "ΦΙΛΙΠΠΟΥΠΟΛΕΩΣ",
                                    :to "01:00 μμ"}
                                   {:to-street "ΣΤΡ. ΝΙΔΕΡ",
                                    :from-street "ΣΩΚΙΩΝ",
                                    :from "08:00 πμ",
                                    :affected-numbers "Μονά/Ζυγά",
                                    :street "ΣΤΑΥΡΟΥΠΟΛΕΩΣ",
                                    :to "01:00 μμ"}
                                   {:to-street "ΔΑΡΔΑΝΕΛΛΙΩΝ",
                                    :from-street "ΣΤΡ. ΝΙΔΕΡ",
                                    :from "08:00 πμ",
                                    :affected-numbers "Μονά/Ζυγά",
                                    :street "ΣΩΚΙΩΝ",
                                    :to "01:00 μμ"}],
                  :note-id        "474",
                  :cause          "Κατασκευές"
                  :affected-areas-raw "Μονά/Ζυγά οδός:ΒΟΣΠΟΡΟΥ απο κάθετο: ΒΟΣΠΟΡΟΥ Ν0 59 έως κάθετο: ΜΕΤΡΩΝ από: 08:00 πμ έως: 01:00 μμ\nΜονά/Ζυγά οδός:ΦΙΛΙΠΠΟΥΠΟΛΕΩΣ απο κάθετο: ΜΕΤΡΩΝ έως κάθετο: ΣΩΚΙΩΝ από: 08:00 πμ έως: 01:00 μμ\nΜονά/Ζυγά οδός:ΣΤΑΥΡΟΥΠΟΛΕΩΣ απο κάθετο: ΣΩΚΙΩΝ έως κάθετο: ΣΤΡ. ΝΙΔΕΡ από: 08:00 πμ έως: 01:00 μμ\nΜονά/Ζυγά οδός:ΣΩΚΙΩΝ απο κάθετο: ΣΤΡ. ΝΙΔΕΡ  έως κάθετο: ΔΑΡΔΑΝΕΛΛΙΩΝ από: 08:00 πμ έως: 01:00 μμ"}]]
    (with-redefs [;t/local-time second
                  ;t/local-date-time second
                 sut/str->time (fn [a & b] a)
                  sut/str->datetime (fn [a & b] a)]
      (is (= outages
             (sut/outages dom))))))
