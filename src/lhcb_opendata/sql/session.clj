(ns lhcb-opendata.sql.session
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.sql SparkSession]))

(defn- apply-config
  [builder config]
  (reduce-kv (fn [builder config-key config-val]
               (.config builder config-key config-val))
             builder
             config))

(defn spark-session
  [{:keys [master app-name enable-hive config]}]
  {:pre [(some? app-name)]}
  (cond-> (SparkSession/builder)
    master      (.master master)
    app-name    (.appName app-name)
    config      (apply-config config)
    enable-hive (.enableHiveSupport)
    true        (.getOrCreate)))

(defn spark-context
  [^SparkSession spark-session]
  (.sparkContext spark-session))

(defn java-spark-context
  "Return a JavaSparkContext that can be used by the sparkling.core functions."
  [^SparkSession spark-session]
  (JavaSparkContext. (.sparkContext spark-session)))

(defn sql-context
  "Returns an SQLContext that can be used to set SparkSQL configuration properties."
  [^SparkSession spark-session]
  (.sqlContext spark-session))

(defn version
  [^SparkSession spark-session]
  (.version spark-session))
