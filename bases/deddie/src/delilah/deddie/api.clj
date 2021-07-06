(ns delilah.deddie.api
  (:require [clojure.spec.alpha :as s]

            [delilah.common-parser.interface :as cparser]
            [delilah.deddie.spec]
            [delilah.deddie.scraper :as scraper]))

(def endpoint "https://siteapps.deddie.gr/Outages2Public")
(def partial-endpoint "https://siteapps.deddie.gr/Outages2Public/Home/OutagesPartial")

(defn prefectures []
  (-> endpoint
      (slurp)
      (cparser/parse)
      (scraper/prefectures)))
(s/fdef prefectures
  :ret (s/coll-of :deddie/prefecture))

(defn prefecture-name->id [name]
  (->> (prefectures)
       (filter #(= (:deddie.prefecture/name %) name))
       (first)
       (:deddie.prefecture/id)))
(s/fdef prefecture-name->id
  :args (s/cat :name :deddie.prefecture/name)
  :ret :deddie.prefecture/id)

(defn municipalities [prefecture]
  (let [prefecture-id  (if (number? prefecture)
                           prefecture
                           (prefecture-name->id prefecture))
        municipalities (-> (str endpoint "?PrefectureID=" prefecture-id)
                           (slurp)
                           (cparser/parse)
                           (scraper/municipalities))]
    (map #(assoc % :deddie.prefecture/id prefecture-id) municipalities)))
(s/fdef municipalities
  :args (s/cat :prefecture #(or (string? %) (integer? %)))
  :ret (s/coll-of :deddie/municipality))

(defn municipality-name->id [name prefecture-id]
  (->> (municipalities prefecture-id)
       (filter #(= (:deddie.municipality/name %) name))
       (first)
       (:deddie.municipality/id)))
(s/fdef municipality-name->id
  :args (s/cat :name :deddie.municipality/name
               :prefecture-id :deddie.prefecture/id)
  :ret :deddie.prefecture/id)

(defn all-municipalities []
  (let [prefs  (prefectures)
        mapper (zipmap (map :deddie.prefecture/id prefs)
                       (map :deddie.prefecture/name prefs))]
    (->> (map :deddie.prefecture/id prefs)
         (mapcat municipalities)
         (map #(assoc % :deddie.prefecture/name (mapper (:deddie.prefecture/id %)))))))
(s/fdef all-municipalities
  :ret (s/coll-of :deddie/municipality))

(defn dom [{:keys [prefecture municipality page]}]
  (let [page            (or page 1)
        prefecture-id   (if (number? prefecture)
                          prefecture
                          (prefecture-name->id prefecture))
        municipality-id (if (or (number? municipality) (nil? municipality))
                          municipality
                          (municipality-name->id municipality prefecture-id))]
    (-> (format "%s?PrefectureID=%s&MunicipalityID=%s&page=%s" endpoint prefecture-id municipality-id page)
        (slurp)
        (cparser/parse))))


(defn outages
  ([prefecture]
   (outages prefecture nil))
  ([prefecture municipality]
   (loop [all-outages     []
          partial-outages []
          page            1]
     (if (and (empty? partial-outages)
              (not-empty all-outages))
       all-outages
       (recur (concat all-outages partial-outages)
              (-> (dom {:prefecture prefecture
                        :municipality municipality
                        :page page})
                  scraper/outages)
              (inc page))))))


(comment
  (def active-prefecture-1 (-> (dom {:prefecture 10}) scraper/prefecture))
  (def active-prefecture-2 (-> (dom {:prefecture "ΑΤΤΙΚΗΣ"}) scraper/prefecture))

  (def all-prefectures (prefectures))

  (def active-municipality-1 (scraper/municipality (dom {:prefecture 10})))
  (def active-municipality-2 (scraper/municipality (dom {:prefecture 10 :municipality "ΑΘΗΝΑΙΩΝ"})))
  (def active-municipality-3 (scraper/municipality (dom {:prefecture "ΘΕΣΣΑΛΟΝΙΚΗΣ" :municipality "ΘΕΣΣΑΛΟΝΙΚΗΣ"})))

  (def municipalities-1 (municipalities 23))
  (def municipalities-2 (municipalities "ΘΕΣΣΑΛΟΝΙΚΗΣ"))

  (def municipalities-all (all-municipalities))

  ; Upcoming outages within the municipality of Athens
  (def outages-map-1 (outages 10 112))
  (def outages-map-2 (outages "ΑΤΤΙΚΗΣ" "ΑΘΗΝΑΙΩΝ"))

  ; Upcoming outages within the entire prefecture of Thessaloniki
  (def outages-map-3 (outages 23))
  (def outages-map-4 (outages "ΘΕΣΣΑΛΟΝΙΚΗΣ"))

  ; Upcoming outages within the municipality of Thessaloniki
  (def outages-map-5 (outages 23 454))
  (def outages-map-6 (outages "ΘΕΣΣΑΛΟΝΙΚΗΣ" "ΘΕΣΣΑΛΟΝΙΚΗΣ"))

  (def num-pages-7 (-> (dom {:prefecture "ΑΤΤΙΚΗΣ"})
                     scraper/num-pages))

  (def num-pages-8 (-> (dom {:prefecture "ΘΕΣΣΑΛΟΝΙΚΗΣ"})
                     scraper/num-pages)))
