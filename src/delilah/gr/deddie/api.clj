(ns delilah.gr.deddie.api
  (:require [hickory.select :as hs]

            [delilah.common.parser :as cparser]
            [delilah.gr.deddie.parser :as p]))

(defn prefectures []
  (-> "https://siteapps.deddie.gr/Outages2Public"
      slurp
      cparser/parse
      p/prefectures))

(defn municipalities [prefecture-id]
  (-> (str "https://siteapps.deddie.gr/Outages2Public?PrefectureID=" prefecture-id)
      slurp
      cparser/parse
      (p/municipalities prefecture-id)))

(defn dom
  ([prefecture-id]
   (dom prefecture-id nil))
  ([prefecture-id municipality-id]
   (-> (format "https://siteapps.deddie.gr/Outages2Public?PrefectureID=%s&MunicipalityID=%s" prefecture-id municipality-id)
       slurp
       cparser/parse)))

(defn outages [dom]
  (p/outages dom))

(comment
  (def active-prefecture (-> 10 dom p/prefecture))

  (def all-prefectures (prefectures))

  (def active-municipality (p/municipality (dom 10 112)))

  (def all-municipalities
    (let [prefectures (prefectures)
          mapper      (zipmap (map :prefecture/id prefectures)
                              (map :prefecture/name prefectures))]
      (->> (mapcat #(municipalities (:prefecture/id %)) prefectures)
           (map #(assoc % :prefecture/name (mapper (:prefecture/id %)))))))

  ; Get outages for the municipality of Athens
  (def outages-map (p/outages (dom 10 112)))

  ; Get outages for the entire prefecture of Thessaloniki
  (def outages-map (-> 23 dom p/outages))

  (def area-desc
    (-> outages-map
        first
        :affected-areas
        #_first))

  (-> area-desc first)
  (map affected-areas area-desc))
