(ns delilah.gr.deddie.scraper-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [clojure.tools.reader.edn :as edn]

            [hickory.select :as hs]

            [delilah.gr.deddie.scraper :as sut]
            [delilah.gr.deddie.parser :as parser]
            [delilah.gr.deddie.api :as api]))

(def live-dom1 (api/dom {:prefecture "ΑΤΤΙΚΗΣ"}))
(def live-dom2 (api/dom {:prefecture "ΑΤΤΙΚΗΣ" :municipality "ΑΘΗΝΑΙΩΝ"}))

(def dom (-> "gr/deddie/dom.edn"
             io/resource
             slurp
             edn/read-string))

(deftest test-area-id
  (is (= 10
         (-> (hs/select (:prefecture sut/selectors) dom)
             first
             (sut/area-id)))))

(deftest test-num-pages
  (let [num-pages (sut/num-pages live-dom1)]
    (is (integer? num-pages))
    (is (> num-pages 1))))

(deftest test-prefecture
  (testing "static dom"
    (is (= {:deddie.prefecture/id 10
            :deddie.prefecture/name "ΑΤΤΙΚΗΣ"}
           (sut/prefecture dom))))
  (testing "live dom"
    (is (= {:deddie.prefecture/id 10
            :deddie.prefecture/name "ΑΤΤΙΚΗΣ"}
           (sut/prefecture live-dom1)
           (sut/prefecture live-dom2)))))

(deftest test-municipality
  (testing "static dom"
    (is (= {:deddie.municipality/id 164
            :deddie.municipality/name "Ν.ΣΜΥΡΝΗΣ"}
           (sut/municipality dom))))
  (testing "live dom"
    (is (= {:deddie.municipality/id 112
            :deddie.municipality/name "ΑΘΗΝΑΙΩΝ"}
           (sut/municipality live-dom2)))
    (is (= nil
           (sut/municipality live-dom1)))))

(deftest test-outages
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
    (with-redefs [parser/str->time     (fn [a & b] a)
                  parser/str->datetime (fn [a & b] a)]
      (is (= outages
             (sut/outages dom))))))
