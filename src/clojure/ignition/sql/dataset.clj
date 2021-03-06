(ns ignition.sql.dataset
  (:refer-clojure :exclude [count first group-by map read sort take])
  (:require [camel-snake-kebab
             [core :refer [->camelCaseString ->snake_case_string ->SCREAMING_SNAKE_CASE_STRING]]
             [extras :refer [transform-keys]]]
            [ignition.sql.function :refer [map-function]])
  (:import [java.util Properties]
           [org.apache.spark.sql Column Dataset Encoder SparkSession SaveMode]))

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
  ;; There seems to be a bug where we can only read a single path.
  (let [[[options] paths] (split-with map? args)]
    (.. (cond-> (.read session)
          options (.options (transform-keys ->camelCaseString options)))
        (format "org.dianahep.sparkroot")
        #_(load (into-array String paths))
        (load (clojure.core/first paths)))))

(def save-mode
  {:overwrite SaveMode/Overwrite
   :append    SaveMode/Append
   :ignore    SaveMode/Ignore
   :error     SaveMode/ErrorIfExists})

(defn write-json
  [^Dataset dataset path]
  (.json (.write dataset) path))

(defn write-parquet
  [^Dataset dataset path & [{:keys [mode partition-by sort-by options]}]]
  {:pre [(some? path) (or (nil? mode) (save-mode mode))]}
  (cond-> (.write dataset)
    mode         (.mode (save-mode mode))
    partition-by (.partitionBy (into-array String partition-by))
    sort-by      (.sortBy (into-array String sort-by))
    options      (.options (transform-keys ->camelCaseString options))
    true         (.parquet path)))

;; ---- API
(defn cache
  "Persist this Dataset with the default storage level (MEMORY_AND_DISK)."
  [^Dataset dataset]
  (.cache dataset))

(defn col
  "Selects a column based on the column name and retruns it as a Column."
  [^Dataset dataset ^String column-name]
  (.col dataset column-name))

(defn columns
  "Returns a sequence that contains all of the Rows in this Dataset."
  [^Dataset dataset]
  (seq (.columns dataset)))

(defn count
  [^Dataset dataset]
  (.count dataset))

(defn describe
  "Computes statistics for numeric and string columns, including
  count, mean, stddev, min, and max."
  [^Dataset dataset & column-names]
  {:pre [(seq column-names)]}
  (.describe dataset (into-array String column-names)))

(defn first
  "Returns the first row."
  [^Dataset dataset]
  (.first dataset))

(defn group-by
  "Groups the Dataset using the specified columns, so that we can run aggregation on them."
  [^Dataset dataset & column-names]
  {:pre [(seq column-names)]}
  (.groupBy dataset (into-array Column (clojure.core/map (partial col dataset) column-names))))

(defn map
  [^Dataset dataset f ^Encoder encoder]
  (.map dataset (map-function f) encoder))

(defn print-schema
  "Prints the schema to the console in a nice tree format."
  [^Dataset dataset]
  (.printSchema dataset))

(defn sample
  ([^Dataset dataset fraction]
   (.sample dataset fraction))
  ([^Dataset dataset fraction seed]
   (.sample dataset fraction seed)))

(defn schema
  [^Dataset dataset]
  (.schema dataset))

(defn select
  "Selects a set of columns."
  [^Dataset dataset columns]
  {:pre [(seq columns)]}
  (if (string? (clojure.core/first columns))
    (.select dataset (clojure.core/first columns) (into-array String (rest columns)))
    (.select dataset (into-array Column columns))))

(defn select-expr
  "Selects a set of SQL expressions."
  [^Dataset dataset sql-exprs]
  {:pre [(seq sql-exprs)]}
  (.selectExpr dataset (into-array String sql-exprs)))

(defn show
  ([^Dataset dataset]
   (.show dataset))
  ([^Dataset dataset n]
   (.show dataset n))
  ([^Dataset dataset n col-width]
   "Shows n rows from the dataset, horizontal rows with a specified column width.

   Pass 0 for unlimited column width."
   (.show dataset n col-width))
  ([^Dataset dataset n col-width vertical?]
   "Shows n rows from the dataset with a specified column width, boolean controls whether to have vertical or horizontal row display.

   Pass 0 for unlimited column width."
   (.show dataset n col-width vertical?)))

(defn stat
  [^Dataset dataset]
  (.stat dataset))

(defn sort
  "Returns a new Dataset sorted by the specified columns, all in
  ascending order, or expressions."
  [^Dataset dataset & column-names-or-exprs]
  {:pre [(seq column-names-or-exprs)]}
  (.sort dataset (into-array column-names-or-exprs)))

(defn summary
  "Computes specified statistics for numeric and string columns.

  Available statistics are:
  - count
  - mean
  - stddev
  - min
  - max
  - arbitrary approximate percentiles specified as a percentage (eg, 75%)

  If no statistics are given, this function computes count, mean, stddev, min,
  approximate quartiles (percentiles at 25%, 50%, and 75%), and max.

  This function is meant for exploratory data analysis, as we make no guarantee
  about the backward compatibility of the schema of the resulting Dataset. If
  you want to programmatically compute summary statistics, use the agg function
  instead."
  [^Dataset dataset & statistics]
  {:pre [(seq statistics)]}
  (.summary dataset (into-array String statistics)))

(defn take
  "Returns the first n rows in the Dataset."
  [^Dataset dataset n]
  (seq (.take dataset n)))

(defn where
  "Filters the dataset using either column or SQL string expression."
  [^Dataset dataset expr]
  (.where dataset expr))

(defn with-column
  [^Dataset dataset ^String name ^Column col]
  (.withColumn dataset name col))
