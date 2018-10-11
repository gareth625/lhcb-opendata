(ns ignition.sql.dataframe-stat-functions
  (:import [org.apache.spark.sql Dataset]))

(defn approx-quantile
  "Calculates the approximate quantiles of a numerical column of a DataFrame."
  [^Dataset ds ^String col-name probabilities relative-error]
  (-> ds
      (.approxQuantile col-name (double-array probabilities) relative-error)
      (vec)))
