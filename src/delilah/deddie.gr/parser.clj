(ns delilah.deddie.gr.parser
  (:require [clojure.tools.logging :as log]
            [clojure.walk :as walk]

            [etaoin.api :as api]
            [hickory.select :as hs]
            [java-time :as t]

            [delilah.common.parser :as cparser]
            [clojure.walk :as walk]
            [clojure.string :as str]))

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
     (let [municipality-elem [{:tag :select :id :MunicipalityID} {:tag :option :fn/text municipality}]]
       (api/wait-visible driver municipality-elem)
       (api/click driver municipality-elem)
       (api/wait-predicate #(api/selected? driver municipality-elem))))
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

(defn format-gr-time [time]
  (-> time
      clojure.string/upper-case
      (clojure.string/replace #"ΠΜ" "AM")
      (clojure.string/replace #"ΜΜ" "PM")))

(defn str->datetime
    ([datetime]
     (str->datetime datetime "d/M/yyyy h:mm:ss a"))
    ([datetime formatter]
     (log/info (format "Parsing datetime string %s as %s" datetime formatter))
     (-> datetime
         format-gr-time
         (#(t/local-date-time formatter %)))))

(defn str->time
  ([time]
   (str->time time "hh:mm a"))
  ([time formatter]
   (log/info (format "Parsing datetime string %s as %s" time formatter))
   (-> time
       format-gr-time
       (#(t/local-time formatter %)))))

(defn deaccent [str]
  "Remove accent from string"
  ;; http://www.matt-reid.co.uk/blog_post.php?id=69
  (let [normalized (java.text.Normalizer/normalize str java.text.Normalizer$Form/NFD)]
    (clojure.string/replace normalized #"\p{InCombiningDiacriticalMarks}+" "")))

(defn affected-area [area-text]
  (cond
    (or (str/starts-with? area-text "Μονά")
        (str/starts-with? area-text "Ζυγά"))
    (let [split-area-text   (-> (str "affected-numbers\n" area-text)
                                (clojure.string/replace "οδός:" "\nstreet\n")
                                (clojure.string/replace "από:" "\nfrom\n")
                                (clojure.string/replace "έως:" "\nto\n")
                                (clojure.string/replace "απο κάθετο:" "\nfrom-street\n")
                                (clojure.string/replace "έως κάθετο:" "\nto-street\n")
                                clojure.string/split-lines)
          affected-area-map (->> split-area-text
                                 (map clojure.string/trim)
                                 (apply hash-map)
                                 walk/keywordize-keys)
          parse-times-fn    (fn [time] (when time (str->datetime time "hh:mm a")))]
      (->> affected-area-map
           #_(map #(assoc % :from (parse-times-fn (:from %))))
           #_(map #(assoc % :to (parse-times-fn (:to %))))))

    :else
    area-text))

(defn affected-areas [all-areas-text]
  (->> (clojure.string/split all-areas-text #"\n")
       (map affected-area)))

(defn affected-area-test []
  (let [area-text "Μονά      οδός:ΠΕΡΓΑΜΟΥ απο: ΠΕΡΓΑΜΟΥ ΝΟ 19 έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ έως κάθετο: ΙΩΝΙΑΣ από: 07:30 πμ έως: 02:30 μμ\nΖυγά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΕΦΕΣΟΥ ΝΟ 26 έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΕΦΕΣΟΥ απο κάθετο: ΚΟΥΚΛΟΥΤΖΑ έως: ΕΦΕΣΟΥ ΝΟ 35 από: 07:30 πμ έως: 02:30 μμ\nΜονά/Ζυγά οδός:ΑΙΓΑΙΟΥ απο κάθετο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ έως: ΑΙΓΑΙΟΥ ΝΟ 28 από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ απο: ΚΩΝΣΤΑΝΤΙΝΟΥΠΟΛΩΣ ΝΟ 7 έως κάθετο: ΕΦΕΣΟΥ από: 07:30 πμ έως: 02:30 μμ\nΜονά/Ζυγά οδός:ΙΩΝΙΑΣ απο κάθετο: ΠΕΡΓΑΜΟΥ έως κάθετο: ΑΙΓΑΙΟΥ από: 07:30 πμ έως: 02:30 μμ\nΜονά      οδός:ΒΟΥΡΝΟΒΑ απο κάθετο: ΑΙΓΑΙΟΥ έως κάθετο: ΘΡΑΚΗΣ από: 07:30 πμ έως: 02:30 μμ"]
    #_(= nil
       (affected-area area-text))
    (affected-areas area-text)))

(defn outage [o]
  (let [outage-map (zipmap [:start :end :municipality :affected-areas :note-id :cause] o)]
    (-> outage-map
        (update :start str->datetime)
        (update :end str->datetime)
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
        #_first))

  (-> area-desc first)
  (map affected-areas area-desc)

  (api/quit driver))
