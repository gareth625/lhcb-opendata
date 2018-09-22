(ns sparkling.sql.dataset
  (:refer-clojure :exclude [read])
  (:require [camel-snake-kebab
             [core :refer [->camelCaseString ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
             [extras :refer [transform-keys]]])
  (:import [java.util Properties]
           [org.apache.spark.sql Dataset SparkSession SaveMode]))

(defn read-csv
  [^SparkSession session & args]
  (let [[[options] paths] (split-with map? args)
        schema            (:schema options)
        options           (not-empty (dissoc options :schema))]
    (cond-> (.read session)
      options (.options (transform-keys ->camelCaseString options))
      schema  (.schema schema)
      true    (.csv (into-array String paths)))))

(defn read-jdbc
  [^SparkSession session url table properties]
  {:pre [(:user properties) (:password properties)]}
  (let [prop (reduce (fn [p [k v]] (.put p (->camelCaseString k) v) p)
                     (Properties.)
                     properties)]
    (.jdbc (.read session) url table prop)))

(defn read-json
  [^SparkSession session & args]
  (let [[[options] paths] (split-with map? args)
        schema            (:schema options)
        options           (not-empty (dissoc options :schema))]
    (cond-> (.read session)
      options (.options (transform-keys ->camelCaseString options))
      schema  (.schema schema)
      true    (.json (into-array String paths)))))

(defn read-parquet
  [^SparkSession session & args]
  (let [[[options] paths] (split-with map? args)]
    (cond-> (.read session)
      options (.options (transform-keys ->camelCaseString options))
      true    (.parquet (into-array String paths)))))

(defn read-root
  [^SparkSession session & args]
  (let [[[options] paths] (split-with map? args)]
    (println options paths)
    (.. (cond-> (.read session)
          options (.options (transform-keys ->camelCaseString options)))
        (format "org.dianahep.sparkroot")
        #_(load (into-array String paths))
        (load (first paths)))))

(def save-mode
  {:overwrite SaveMode/Overwrite
   :append    SaveMode/Append
   :ignore    SaveMode/Ignore
   :error     SaveMode/ErrorIfExists})

(defn write-parquet
  [^Dataset dataset path & [{:keys [mode partition-by sort-by options]}]]
  {:pre [(some? path) (or (nil? mode) (save-mode mode))]}
  (cond-> (.write dataset)
    mode         (.mode (save-mode mode))
    partition-by (.partitionBy (into-array String partition-by))
    sort-by      (.sortBy (into-array String sort-by))
    options      (.options (transform-keys ->camelCaseString options))
    true         (.parquet path)))

(defn show
  ([^Dataset dataset]
   (.show dataset))
  ([n ^Dataset dataset]
   (.show dataset n))
  ([n col-width ^Dataset dataset]
   "Shows n rows from the dataset, horizontal rows with a specified column width.

   Pass 0 for unlimited column width."
   (.show dataset n col-width))
  ([n col-width vertical? ^Dataset dataset]
   "Shows n rows from the dataset with a specified column width, boolean controls whether to have vertical or horizontal row display.

   Pass 0 for unlimited column width."
   (println n col-width vertical? dataset)
   (.show dataset n col-width vertical?)))
