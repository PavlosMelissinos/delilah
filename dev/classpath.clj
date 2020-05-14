(ns classpath
  (:require [clojure.java.io :as io]))

(defn- root-dynclass-loader []
  (last
    (take-while
      #(instance? clojure.lang.DynamicClassLoader %)
      (iterate #(.getParent ^java.lang.ClassLoader %) (.getContextClassLoader (Thread/currentThread))))))

(defn- ensure-dynclass-loader! []
  (let [cl (.getContextClassLoader (Thread/currentThread))]
    (when-not (instance? clojure.lang.DynamicClassLoader cl)
      (.setContextClassLoader (Thread/currentThread) (clojure.lang.DynamicClassLoader. cl)))))

(defn add! [filename]
  (ensure-dynclass-loader!)
  (let [root-loader (root-dynclass-loader)]
    (.addURL ^clojure.lang.DynamicClassLoader root-loader (.toURL (io/file "target")))))
