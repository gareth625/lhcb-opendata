(ns ignition.sql.row
  (:refer-clojure :exclude [get])
  (:import [org.apache.spark.sql Row]))

(defn get
  "Returns the object at index `i`."
  [^Row r i]
  (.get r i))

(defn get-as
  "Returns the value at position `x`. The position can either be an index or field name."
  [^Row r x]
  (.getAs r x))

(defn field-index
  "Returns the index of the named field."
  [^Row r ^String s]
  (.fieldIndex r s))

(defn length
  "The number of elements in the Row."
  [^Row r]
  (.length r))

(defn schema
  "Schema for the row."
  [^Row r]
  (.schema r))

(defn size
  "The number of elements in the Row."
  [^Row r]
  (.size r))
