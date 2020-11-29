(ns delilah.gr.deddie.api
  (:require [delilah.common.parser :as cparser]
            [delilah.gr.deddie.parser :as p]
            [delilah.gr.deddie.scraper :as s]))

(def endpoint "https://siteapps.deddie.gr/Outages2Public")
(def partial-endpoint "https://siteapps.deddie.gr/Outages2Public/Home/OutagesPartial")

(defn prefectures []
  (-> endpoint
      (slurp)
      (cparser/parse)
      (s/prefectures)))

(defn prefecture-name->id [name]
  (->> (prefectures)
       (filter #(= (:deddie.prefecture/name %) name))
       (first)
       (:deddie.prefecture/id)
       Integer/parseInt))

(defn municipalities [prefecture]
  (let [prefecture-id (if (number? prefecture)
                           prefecture
                           (prefecture-name->id prefecture))]
    (map #(assoc % :deddie.prefecture/id prefecture-id)
         (-> (str endpoint "?PrefectureID=" prefecture-id)
             (slurp)
             (cparser/parse)
             (s/municipalities)))))

(defn municipality-name->id [name prefecture-id]
  (->> (municipalities prefecture-id)
       (filter #(= (:deddie.municipality/name %) name))
       (first)
       (:deddie.municipality/id)
       Integer/parseInt))

(defn all-municipalities []
  (let [prefectures (prefectures)
        mapper      (zipmap (map :deddie.prefecture/id prefectures)
                            (map :deddie.prefecture/name prefectures))]
    (->> (map :deddie.prefecture/id prefectures)
         (mapcat municipalities)
         (map #(assoc % :deddie.prefecture/name (mapper (:deddie.prefecture/id %)))))))

(defn dom
  ([prefecture]
   (dom prefecture nil 1))
  ([prefecture municipality]
   (dom prefecture municipality 1))
  ([prefecture municipality page]
   (let [prefecture-id   (if (number? prefecture)
                           prefecture
                           (-> prefecture prefecture-name->id))
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
  (def active-prefecture (-> 10 dom p/prefecture))
  (def active-prefecture (-> "ΑΤΤΙΚΗΣ" dom p/prefecture))

  (def all-prefectures (prefectures))

  (def active-municipality (s/municipality (dom 10)))
  (def active-municipality (s/municipality (dom 10 "ΑΘΗΝΑΙΩΝ")))
  (def active-municipality (s/municipality (dom "ΘΕΣΣΑΛΟΝΙΚΗΣ" "ΘΕΣΣΑΛΟΝΙΚΗΣ")))

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
