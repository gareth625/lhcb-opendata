(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject lhcb-opendata "0.1.0-SNAPSHOT"
  :description "Demonstrating the LHCb OpenData analysis in Clojure and Spark."
  :url ""
  :main lhcb-opendata
  :aot :all
  :target-path "target/%s"
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.9.0"]

                 [camel-snake-kebab "0.4.0"]
                 [gorillalabs/sparkling "2.1.3"]
                 [org.clojars.runa/cliopatra "1.1.0"]
                 [org.diana-hep/spark-root_2.11 "0.1.16" :exclusions [org.apache.logging.log4j/log4j]]
                 [prismatic/schema "1.1.9"]]
  :profiles {:provided {:dependencies [[com.fasterxml.jackson.core/jackson-annotations "2.6.7"]
                                       [com.fasterxml.jackson.core/jackson-core "2.6.7"]
                                       [com.fasterxml.jackson.core/jackson-databind "2.6.7"]
                                       [org.apache.spark/spark-core_2.11 "2.3.1"
                                        :exclusions [com.fasterxml.jackson.core/jackson-annotations
                                                     com.fasterxml.jackson.core/jackson-core
                                                     com.fasterxml.jackson.core/jackson-databind
                                                     commons-codec
                                                     org.apache.httpcomponents/httpclient
                                                     org.apache.httpcomponents/httpcore
                                                     joda-time]]
                                       [org.apache.spark/spark-sql_2.11 "2.3.1"
                                        :exclusions [com.fasterxml.jackson.core/jackson-annotations
                                                     com.fasterxml.jackson.core/jackson-core
                                                     com.fasterxml.jackson.core/jackson-databind
                                                     commons-codec
                                                     org.apache.httpcomponents/httpclient
                                                     org.apache.httpcomponents/httpcore
                                                     joda-time]]
                                       [org.apache.spark/spark-hive_2.11 "2.3.1"
                                        :exclusions [com.fasterxml.jackson.core/jackson-annotations
                                                     com.fasterxml.jackson.core/jackson-core
                                                     com.fasterxml.jackson.core/jackson-databind
                                                     commons-codec
                                                     org.apache.httpcomponents/httpclient
                                                     org.apache.httpcomponents/httpcore
                                                     joda-time]]
                                       [org.apache.hadoop/hadoop-client "2.8.4"
                                        :exclusions [com.fasterxml.jackson.core/jackson-annotations
                                                     com.fasterxml.jackson.core/jackson-core
                                                     com.fasterxml.jackson.core/jackson-databind
                                                     commons-codec
                                                     org.apache.httpcomponents/httpclient
                                                     org.apache.httpcomponents/httpcore
                                                     joda-time]]
                                       [org.apache.hadoop/hadoop-common "2.8.4"
                                        :exclusions [com.fasterxml.jackson.core/jackson-annotations
                                                     com.fasterxml.jackson.core/jackson-core
                                                     com.fasterxml.jackson.core/jackson-databind
                                                     commons-codec
                                                     org.apache.httpcomponents/httpclient
                                                     org.apache.httpcomponents/httpcore
                                                     joda-time]]
                                       [org.apache.hadoop/hadoop-aws "2.8.4"
                                        :exclusions [javax.servlet/servlet-api
                                                     javax.servlet.jsp/jsp-api
                                                     org.mortbay.jetty/servlet-api]]]}
             :uberjar {:aot :all}
             :dev {:jvm-opts ["-Xmx3g"]
                   :source-paths ["dev"]}})
