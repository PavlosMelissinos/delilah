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

(deftest test-deaccent
  (is (= "γαιδουρι"
         (sut/deaccent "γαϊδούρι"))))


(deftest cleanup
  (is (= "ΚΡΩΠΙΑΣ"
         (sut/cleanup {:type :element,
                       :attrs {:class "col-xs-2"},
                       :tag :td,
                       :content ["\r\nΚΡΩΠΙΑΣ                        "]})))
  (is (= "6/12/2020 3:00:00 μμ"
         (sut/cleanup  {:type :element,
                        :attrs {:class "col-xs-1"},
                        :tag :td,
                        :content ["6/12/2020 3:00:00 μμ"]}))))

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
        fmt-time-fn (fn [tm] (t/format "HH:mm" tm))]
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
