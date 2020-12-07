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

(deftest test-query-map
  (is (= {:a ["1"]
          :b ["2"]}
         (sut/query-map "a=1&b=2")))

  (is (= {:non-query-form [""]}
         (sut/query-map "non-query-form"))))


(deftest test-page-num
  (is (= 2
         (sut/page-num "http://doma.in/api?arg1=a&page=2"))))

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


(deftest test-cleanup
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

(deftest test-enforce-even
  (is (= ["a" 1 "b" 2 "c" ""]
         (sut/enforce-even ["a" 1 "b" 2 "c"])
         (sut/enforce-even '("a" 1 "b" 2 "c"))
         (sut/enforce-even ["a" 1 "b" 2 "c" ""])
         (sut/enforce-even '("a" 1 "b" 2 "c" "")))))

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
  (is (= {:to-street "ΕΡ.ΣΤΑΥΡΟΥ",
          :from-street "Κ.ΠΑΛΑΜΑ",
          :from "",
          :affected-numbers "Μονά",
          :street "ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ"}
         (@#'sut/parse-area-text "Μονά οδός:ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ απο κάθετο: Κ.ΠΑΛΑΜΑ έως κάθετο: ΕΡ.ΣΤΑΥΡΟΥ από:"))))

(deftest test-affected-area
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
         (@#'sut/affected-area "Μονά      οδός:ΒΟΥΡΝΟΒΑ απο κάθετο: ΑΙΓΑΙΟΥ έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ")))

  (is (= {:to-street "ΕΡ.ΣΤΑΥΡΟΥ",
          :from-street "Κ.ΠΑΛΑΜΑ",
          :from nil,
          :affected-numbers "Μονά",
          :street "ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ",
          :to nil}
         (@#'sut/affected-area "Μονά οδός:ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ απο κάθετο: Κ.ΠΑΛΑΜΑ έως κάθετο: ΕΡ.ΣΤΑΥΡΟΥ από:"))))

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
                (map #(update % :to fmt-time-fn))))))

  (testing "Incomplete area text"
    (let [area-text "Μονά/Ζυγά οδός:ΚΑΖΑΝΤΖΑΚΗ απο κάθετο: ΒΕΝΙΖΕΛΟΥ έως κάθετο: ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ από: 08:00 πμ έως: 02:30 μμ\r\nΜονά/Ζυγά οδός:ΤΑΞΙΑΡΧΩΝ απο κάθετο: ΑΝΑΛΗΨΕΩΣ έως κάθετο: ΜΟΝΕΜΒΑΣΙΑΣ από: 08:00 πμ έως: 02:30 μμ\r\nΜονά/Ζυγά οδός:Κ.ΠΑΛΑΜΑ απο κάθετο: ΕΘΝ.ΑΝΤΙΣΤΑΣΕΩΣ έως κάθετο: ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ  από: 08:00 πμ έως: 02:30 μμ\r\nΜονά/Ζυγά οδός:Γ.ΑΦΑΡΑ απο κάθετο: ΒΕΝΙΖΕΛΟΥ έως κάθετο: ΜΟΝΕΜΒΑΣΙΑΣ από: 08:00 πμ έως: 02:30 μμ\r\nΜονά/Ζυγά οδός:ΜΟΝΕΜΒΑΣΙΑΣ απο κάθετο: Γ.ΑΦΑΡΑ  έως κάθετο: ΤΑΞΙΑΡΧΩΝ από: 08:00 πμ έως: 02:30 μμ\r\nΜονά/Ζυγά οδός:ΑΝΑΛΗΨΕΩΣ απο κάθετο: Γ.ΑΦΑΡΑ   έως κάθετο: ΚΑΖΑΝΤΖΑΚΗ  από: 08:00 πμ έως: 02:30 μμ\r\nΜονά/Ζυγά οδός:ΒΕΝΙΖΕΛΟΥ απο κάθετο: ΑΓ.ΦΑΝΟΥΡΙΟΥ έως κάθετο: ΚΑΖΑΝΤΖΑΚΗ  από: 08:00 πμ έως: 02:30 μμ\r\nΜονά      οδός:ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ   απο κάθετο: Κ.ΠΑΛΑΜΑ  έως κάθετο: ΕΡ.ΣΤΑΥΡΟΥ από:"
          fmt-time-fn (fn [tm]
                        (when (not (nil? tm))
                          (t/format "HH:mm" tm)))]
      (is (= [{:to-street "ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ", :from-street "ΒΕΝΙΖΕΛΟΥ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "ΚΑΖΑΝΤΖΑΚΗ", :to "14:30"}
              {:to-street "ΜΟΝΕΜΒΑΣΙΑΣ", :from-street "ΑΝΑΛΗΨΕΩΣ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "ΤΑΞΙΑΡΧΩΝ", :to "14:30"}
              {:to-street "ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ", :from-street "ΕΘΝ.ΑΝΤΙΣΤΑΣΕΩΣ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "Κ.ΠΑΛΑΜΑ", :to "14:30"}
              {:to-street "ΜΟΝΕΜΒΑΣΙΑΣ", :from-street "ΒΕΝΙΖΕΛΟΥ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "Γ.ΑΦΑΡΑ", :to "14:30"}
              {:to-street "ΤΑΞΙΑΡΧΩΝ", :from-street "Γ.ΑΦΑΡΑ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "ΜΟΝΕΜΒΑΣΙΑΣ", :to "14:30"}
              {:to-street "ΚΑΖΑΝΤΖΑΚΗ", :from-street "Γ.ΑΦΑΡΑ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "ΑΝΑΛΗΨΕΩΣ", :to "14:30"}
              {:to-street "ΚΑΖΑΝΤΖΑΚΗ", :from-street "ΑΓ.ΦΑΝΟΥΡΙΟΥ", :from "08:00", :affected-numbers "Μονά/Ζυγά", :street "ΒΕΝΙΖΕΛΟΥ", :to "14:30"}
              {:to-street "ΕΡ.ΣΤΑΥΡΟΥ", :from-street "Κ.ΠΑΛΑΜΑ", :from nil, :affected-numbers "Μονά", :street "ΑΓ.ΠΑΝΤΕΛΕΗΜΟΝΟΣ", :to nil}]
             (->> area-text
                  sut/affected-areas
                  (map #(update % :from fmt-time-fn))
                  (map #(update % :to fmt-time-fn))))))))
