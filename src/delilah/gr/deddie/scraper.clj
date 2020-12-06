(ns delilah.gr.deddie.scraper
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]

            [hickory.select :as hs]

            [delilah.gr.deddie.parser :as parser]))

(def selectors
  {:prefecture     (hs/descendant
                    (hs/id "PrefectureID")
                    (hs/and (hs/tag :option) (hs/attr :selected)))
   :prefectures    (hs/descendant
                    (hs/id "PrefectureID")
                    (hs/tag :option))
   :municipality   (hs/descendant
                    (hs/id "MunicipalityID")
                    (hs/and (hs/tag :option) (hs/attr :selected)))
   :municipalities (hs/descendant
                    (hs/id "MunicipalityID")
                    (hs/tag :option))
   :outages        (hs/descendant
                    (hs/id "tblOutages")
                    (hs/or (hs/tag :thead) (hs/tag :tbody))
                    (hs/or (hs/tag :th) (hs/tag :td)))
   :last-page-link (hs/descendant
                    (hs/class "PagedList-skipToLast")
                    (hs/tag "a"))})

;;; Clean up DOM

(defn area-id [area-dom]
  (let [raw-area-id (-> area-dom :attrs :value)]
    (if (empty? raw-area-id)
      0
      (Integer/parseInt raw-area-id))))

(defn num-pages [dom]
  (let [last-page-href (-> (hs/select (:last-page-link selectors) dom)
                           first
                           :attrs
                           :href)
        last-page-url  (str "https://sitapps.deddie.gr" last-page-href)]
    (if last-page-href
      (parser/page-num last-page-url)
      1)))

(defn prefectures [dom]
  (->> dom
       (hs/select (:prefectures selectors))
       (map #(hash-map :deddie.prefecture/name     (-> % :content first)
                       :deddie.prefecture/id       (area-id %)
                       :deddie.prefecture/selected (-> % :attrs :selected)))))

(defn prefecture [dom]
  (-> (filter :deddie.prefecture/selected (prefectures dom))
      first
      (dissoc :deddie.prefecture/selected)))

(defn municipalities [dom]
  (->> dom
       (hs/select (:municipalities selectors))
       (map #(hash-map :deddie.municipality/name     (-> % :content first)
                       :deddie.municipality/id       (area-id %)
                       :deddie.municipality/selected (-> % :attrs :selected)))))

(defn municipality [dom]
  (-> (filter :deddie.municipality/selected (municipalities dom))
      first
      (dissoc :deddie.municipality/selected)))

(defn outages [dom]
  (let [outages-table (hs/select (:outages selectors) dom)
        labels        (->> outages-table
                           (filter #(= (:tag %) :th))
                           (map parser/cleanup))
        rows          (->> outages-table
                           (filter #(= (:tag %) :td))
                           (map parser/cleanup)
                           (partition (count labels)))]
    (log/info (str "Outage entries: " (vector rows)))
    (map parser/outage rows)))

(comment

  (let [href "/Outages2Public/Home/OutagesPartial?page=14&municipalityID=&prefectureID=10"
        url (str "https://siteapps.deddie.gr" href)]
    (-> url
        parse/page-num)))
