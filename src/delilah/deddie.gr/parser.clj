(ns delilah.deddie.gr.parser
  (:require [clojure.tools.logging :as log]

            [etaoin.api :as api]
            [hickory.select :as hs]
            [java-time :as t]

            [delilah.common.parser :as cparser]))

(defn filter-power-cuts-by
  ([driver county]
   (filter-power-cuts-by county nil))
  ([driver county municipality]
   (log/info "Firing up deddie.gr outages page...")
   (doto driver
     (api/go "https://siteapps.deddie.gr/Outages2Public")
     (api/wait-visible {:id :PrefectureID})
     (api/wait-visible {:id :MunicipalityID}))

   (log/info (format "Navigating to outages for %s/%s..." county municipality))
   (api/click driver [{:tag :select :id :PrefectureID} {:tag :option :fn/text county}])
   (when municipality
     (api/wait-visible driver [{:tag :select :id :MunicipalityID} {:tag :option :fn/text municipality}])
     (api/click driver [{:tag :select :id :MunicipalityID} {:tag :option :fn/text municipality}]))
   (log/info "Got raw outage info!")
   driver))

(defn dom [driver {:keys [county municipality] :as ctx}]
  (-> driver (filter-power-cuts-by county municipality) api/get-source cparser/parse))

;;; Clean up DOM

(defn format-date [date]
  (let [latin-date (-> date
                       clojure.string/upper-case
                       (clojure.string/replace #"ΠΜ" "AM")
                       (clojure.string/replace #"ΜΜ" "PM"))]
    (t/format "dd/MM/YYYY h:m:s a" latin-date)
    #_latin-date))

(def selectors
    {:outages (hs/descendant
               (hs/id "tblOutages")
               (hs/or (hs/tag :thead) (hs/tag :tbody))
               (hs/or (hs/tag :th) (hs/tag :td)))})

(defn cleanup [tbl-entry]
  (log/info (str "Cleaning up table entry " tbl-entry))
  (-> tbl-entry :content first clojure.string/trim))

(defn datetime->iso [datetime]
  (log/info (format "converting datetime string %s to ISO format" datetime))
  (-> datetime
      clojure.string/upper-case
      (clojure.string/replace #"ΠΜ" "AM")
      (clojure.string/replace #"ΜΜ" "PM")
      (#(t/local-date-time "dd/MM/yyyy h:mm:ss a" %))))

(defn deaccent [str]
  "Remove accent from string"
  ;; http://www.matt-reid.co.uk/blog_post.php?id=69
  (let [normalized (java.text.Normalizer/normalize str java.text.Normalizer$Form/NFD)]
    (clojure.string/replace normalized #"\p{InCombiningDiacriticalMarks}+" "")))

(defn affected-area [area-text]
  (let [split-area-text (-> area-text
                            clojure.string/upper-case
                            deaccent
                            (clojure.string/replace "ΟΔΟΣ:" "\n")
                            (clojure.string/replace "ΑΠΟ:" "\n")
                            (clojure.string/replace "ΕΩΣ:" "\n")
                            (clojure.string/replace "ΑΠΟ ΚΑΘΕΤΟ:" "\n")
                            (clojure.string/replace "ΕΩΣ ΚΑΘΕΤΟ:" "\n")
                            clojure.string/split-lines)]
    (->> split-area-text
         (map clojure.string/trim)
         (zipmap [:affected-numbers :street :from-street :to-street :start :end]))))

(defn affected-areas [all-areas-text]
  (->> (clojure.string/split all-areas-text #"\n")
       (map affected-area)))

(defn outage [o]
  (let [outage-map (zipmap [:start :end :municipality :affected-areas :note-id :cause] o)]
    (-> outage-map
        (update :start datetime->iso)
        (update :end datetime->iso)
        (update :affected-areas affected-areas))))

(defn outages [dom]
  (let [outages-table (hs/select (:outages selectors) dom)
        labels        (->> outages-table
                           (filter #(= (:tag %) :th))
                           (map cleanup))
        rows          (->> outages-table
                           (filter #(= (:tag %) :td))
                           (map cleanup)
                           (partition (count labels)))]
    (log/info (str "Outage entries: " (vector rows)))
    (map outage rows)))


(comment
  (def driver (api/firefox {:headless    false
                            :path-driver "resources/webdrivers/geckodriver"}))

  (def outages-map
    (let [ctx {:county "ΑΤΤΙΚΗΣ" :municipality "Ν.ΣΜΥΡΝΗΣ"}]
      (-> (dom driver ctx)
          outages)))

  (def area-desc
    (-> outages-map
        first
        :affected-areas
        first))

  (-> area-desc first)
  (map affected-areas area-desc)

  (api/quit driver))
