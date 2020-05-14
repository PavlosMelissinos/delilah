(ns dev
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.namespace.repl :as ns-tools]
            [clojure.test :as test]
            [clojure.spec.test.alpha :as stest]

            [classpath]
            [integrant.repl :as ig]
            [kaocha.repl :as kaocha]

            [philistine.db]
            [philistine.system :as system]
            [philistine.service]))

(def refresh ns-tools/refresh)
(def refresh-all ns-tools/refresh-all)

(defn instrument-all []
  (stest/instrument (stest/instrumentable-syms)))

(defn unstrument-all []
  (stest/unstrument (stest/instrumentable-syms)))

(defn go []
  (ig/set-prep! (constantly (system/load-config "config.edn")))
  (classpath/add! "target")
  (ig/go))

(defn reset []
  (classpath/add! "target")
  (ig/reset))

(defn halt [] (ig/halt))

(defn sys [] integrant.repl.state/system)
(defn db [] (:db/connection (sys)))
