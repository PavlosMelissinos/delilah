(ns delilah.gr.dei.cookies
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.reader.edn :as edn]

            [etaoin.api :as api]
            [etaoin.keys :as k]
            [me.raynes.fs :as fs]
            [taoensso.timbre :as log]

            [delilah.gr.dei :as dei]))

(defn log-in [driver {::dei/keys [user pass] :as ctx}]
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

(defn location [{:delilah/keys [cache-dir]
                 ::dei/keys [user]
                 :as ctx}]
  (let [cache-dir (fs/expand-home cache-dir)]
    (clojure.string/join "/" [cache-dir "dei" "cookies" user])))

(defn- bake [driver ctx]
  (log/info "Getting fresh cookies from the oven...")
  (let [cookies (-> (log-in driver ctx) api/get-cookies)]
    (-> ctx location fs/parent fs/mkdirs)
    (-> ctx location (spit cookies))
    cookies))

(defn with-session-bake [{:keys [driver] :as ctx}]
  (api/with-driver (:type driver) (dissoc driver :type) d
    (bake d ctx)))

(defn serve [{::dei/keys [user] :as ctx}]
  (log/info "Loading cached cookies...")
  (try
    (-> ctx location slurp edn/read-string)
    (catch Exception e
      (do
        (log/info (str "No cookies found for user" user " at " (location ctx)))
        (with-session-bake ctx)))))

(defn- as-kv-string [{:keys [name value] :as cookie}]
  (clojure.string/join "=" [name value]))

(defn ->string [cookies]
  (clojure.string/join "; " (map as-kv-string cookies)))
