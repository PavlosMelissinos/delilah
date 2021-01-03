(ns delilah.gr.dei.cookies
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.reader.edn :as edn]

            [etaoin.api :as api]
            [etaoin.keys :as k]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]

            [delilah.gr.dei :as dei]))

(s/def :delilah/driver api/firefox?)

(defn- log-in [driver {::dei/keys [user pass]}]
  (log/info "Firing up DEI sign-in page...")
  (doto driver
    (api/go "https://www.dei.gr/EBill/Login.aspx")
    (api/wait-visible {:id :txtUserName}))
  (log/info (format "Signing into DEI account as %s..." user))
  (doto driver
    (api/fill :txtUserName user)
    (api/fill :txtPassword pass k/enter)
    (api/wait-visible {:tag :div :fn/has-class "BillItem"}))
  (log/info "Connected!")
  driver)
(s/fdef log-in
  :args (s/cat :driver :delilah/driver
               :cfg    ::dei/cfg)
  :ret :delilah/driver)

(defn location [{:delilah/keys [cache-dir]
                 ::dei/keys [user]}]
  (let [cache-dir (fs/expand-home cache-dir)]
    (str/join "/" [cache-dir "dei" "cookies" user])))

(defn- bake [driver ctx]
  (log/info "Getting fresh cookies from the oven...")
  (let [cookies (-> (log-in driver ctx) api/get-cookies)]
    (-> ctx location fs/parent fs/mkdirs)
    (-> ctx location (spit cookies))
    cookies))
(s/fdef bake
  :args (s/cat :driver :delilah/driver
               :ctx    (s/keys
                        :req [:delilah/cache-dir])))

(defn with-session-bake [{:keys [driver] :as ctx}]
  (api/with-driver (:type driver) (dissoc driver :type) d
    (bake d ctx)))
(s/fdef with-session-bake
  :args (s/cat :ctx (s/keys
                     :req-un [:delilah/driver])))

(defn serve [{::dei/keys [user] :as ctx}]
  (let [loc (location ctx)]
    (log/info (str "Loading cached cookies from " loc "..."))
    (try
      (-> loc slurp edn/read-string)
      (catch Exception _
        (log/info (str "No cookies found for user" user " at " loc))
        (with-session-bake ctx)))))

(defn- as-kv-string [{:keys [name value]}]
  (str/join "=" [name value]))

(defn ->string [cookies]
  (str/join "; " (map as-kv-string cookies)))
