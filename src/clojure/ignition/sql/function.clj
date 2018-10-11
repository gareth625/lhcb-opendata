(ns ignition.sql.function)

(defn map-function
  [f]
  (new ignition.function.MapFunction f))
