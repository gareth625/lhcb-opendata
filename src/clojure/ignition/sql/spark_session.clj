(ns ignition.sql.spark-session
  (:refer-clojure :exclude [count group-by map read])
  (:require [sparkling.function :as func])
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.sql SparkSession SQLContext]))

(defn- apply-config
  [builder config]
  (reduce-kv (fn [builder config-key config-val]
               (.config builder config-key config-val))
             builder
             config))

(defn close
  "Close the Spark session."
  [^SparkSession session]
  (.close session))

(defn java-spark-context
  "Return a JavaSparkContext that can be used by the sparkling.core functions."
  [^SparkSession session]
  (JavaSparkContext. (.sparkContext session)))

(defn read
  "Returns a DataFrameReader from the Spark session."
  [^SparkSession session]
  (.read session))

(defn spark-context
  "Returns the Spark context from a Spark sessison."
  [^SparkSession session]
  (.sparkContext session))

(defn spark-session
  "Builds a Spark session from the arguments or gets the existing one."
  [{:keys [master app-name enable-hive config]}]
  {:pre [(some? app-name)]}
  (cond-> (SparkSession/builder)
    master      (.master master)
    app-name    (.appName app-name)
    config      (apply-config config)
    enable-hive (.enableHiveSupport)
    true        (.getOrCreate)))

(defn sql-context
  "Returns an SQLContext that can be used to set SparkSQL configuration properties."
  [^SparkSession session]
  (.sqlContext session))

(defn register-udf3
  [name func data-type ^SQLContext sqlc]
  (-> sqlc
      (.udf)
      (.register name (func/function3 func) data-type))
  sqlc)

(defn version
  "Returns the version of Spark in use."
  [^SparkSession session]
  (.version session))
