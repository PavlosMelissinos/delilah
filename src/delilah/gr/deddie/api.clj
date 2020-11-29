(ns delilah.gr.deddie.api
  (:require [clojure.spec.alpha :as s]
            [delilah.common.parser :as cparser]
            [delilah.gr.deddie :as deddie]
            [delilah.gr.deddie.parser :as parser]
            [delilah.gr.deddie.scraper :as scraper]))

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
       (:deddie.prefecture/id)
       (#(if (empty? %) 0 (Integer/parseInt %)))))
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
       (:deddie.municipality/id)
       (#(if (empty? %) 0 (Integer/parseInt %)))))
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

(defn dom
  ([prefecture]
   (dom prefecture nil 1))
  ([prefecture municipality]
   (dom prefecture municipality 1))
  ([prefecture municipality page]
   (let [prefecture-id   (if (number? prefecture)
                           prefecture
                           (prefecture-name->id prefecture))
         municipality-id (if (or (number? municipality) (nil? municipality))
                           municipality
                           (municipality-name->id municipality prefecture-id))]
     (-> (format "%s?PrefectureID=%s&MunicipalityID=%s" endpoint prefecture-id municipality-id)
         (slurp)
         (cparser/parse)))))

(defn outages
  ([prefecture]
   (outages prefecture nil))
  ([prefecture municipality]
   (s/outages (dom prefecture municipality))))


(comment
  (def active-prefecture (-> (dom 10) scraper/prefecture))
  (def active-prefecture (-> (dom "ΑΤΤΙΚΗΣ") scraper/prefecture))

  (def all-prefectures (prefectures))

  (def active-municipality (scraper/municipality (dom 10)))
  (def active-municipality (scraper/municipality (dom 10 "ΑΘΗΝΑΙΩΝ")))
  (def active-municipality (scraper/municipality (dom "ΘΕΣΣΑΛΟΝΙΚΗΣ" "ΘΕΣΣΑΛΟΝΙΚΗΣ")))

  (def municipalities (municipalities 23))
  (def municipalities (municipalities "ΘΕΣΣΑΛΟΝΙΚΗΣ"))

  (def all-municipalities (all-municipalities))

  ; Pending outages within the municipality of Athens
  (def outages-map (outages 10 112))
  (def outages-map (outages "ΑΤΤΙΚΗΣ" "ΑΘΗΝΑΙΩΝ"))

  ; Pending outages within the entire prefecture of Thessaloniki
  (def outages-map (outages 23))
  (def outages-map (outages "ΘΕΣΣΑΛΟΝΙΚΗΣ"))

  ; Pending outages within the municipality of Thessaloniki
  (def outages-map (outages 23 454))
  (def outages-map (outages "ΘΕΣΣΑΛΟΝΙΚΗΣ" "ΘΕΣΣΑΛΟΝΙΚΗΣ")))
