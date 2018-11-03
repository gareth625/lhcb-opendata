# Working Through an LHCb Open Data Set in Clojure and Spark

This project is an example of using [Apache
Spark](https://apache.spark.org) through the
[Clojure](https://clojure.org) programming language. I created the
project a demo of the SparkSQL API and I'm extending it to the
structured streaming API. Both of these use the Spark Dataset API. I
based the project on a workbook by the
[LHCb](http://lhcb-public.web.cern.ch/lhcb-public/) collaboration
which you can find
[here](LHCb](http://lhcb-public.web.cern.ch/lhcb-public/)) in
Python. I was very much inspired by the PySpark walk through
[here](https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Spark_HEP_Examples/LHCb_OpenData_Spark.ipynb).

I gave two talks which reference this project in 2018. One at [Voxx
Days Bristol 2018](https://voxxeddays.com/bristol/)
([Me](https://vxdbristol2018.confinabox.com/talk/YKT-9819/Putting_the_Spark_in_Functional_Fashion_Tech_Analytics)) and the other at [Big Data](https://www.bigdataconference.lt/) in Vilnius ([Me](https://www.bigdataconference.lt/Gareth-Rogers/)).

This project is a work in progress and not a neatly packaged
runnable. Hopefully I'll delete this line. It's also not a physics nor
LHCb analysis tutorial, you should go to the
[source](https://github.com/lhcb/opendata-project/blob/master/LHCb_Open_Data_Project.ipynb)
for that.


## Quick Start

If you know Clojure and already have a Clojure development environment
set up and use [leiningen](https://leiningen.org/) then
* Clone the repo
* Check the `:profile/:dev/:jvm-opts/-Xmx3g` setting is appropriate. This starts a JVM with a 3GB heap size.
* Download the data sets, I have put them in `data/`:
    * PhaseSpaceSimulation.root: http://opendata.cern.ch/eos/opendata/lhcb/AntimatterMatters2017/data/PhaseSpaceSimulation.root)
    * B2HHH_MagnetDown: http://opendata.cern.ch/eos/opendata/lhcb/AntimatterMatters2017/data/B2HHH_MagnetDown.root
    * B2HHH_MagnetUp: http://opendata.cern.ch/eos/opendata/lhcb/AntimatterMatters2017/data/B2HHH_MagnetUp.root
    * The `PhaseSpaceSimulation.root` is the smallest so I recommend starting with that one
* Now you can run
    > lein run --workspace data --dataset simulation --master "local[1]" --filter-selection

where `--workspace` is the location you downloaded the data sets to;
and `--dataset` can be `simulation`, `magnet-down` or
`magnet-up`. Hopefully it's obvious which data set they'll try and
load. The data set names are hard coded and expected to be within the
workspace directory. To use the out of the box settings you'll need to
be able to give your JVM 3G. This is not necessary, it's what I've
hard coded into the `project.clj` file. You can modify this.

The output of `lein run` is a file called `b-mass-plot.json`. This is
a [Vega-Lite](https://vega.github.io/vega-lite/) file and you can copy
and paste it into
[here](https://vega.github.io/editor/#/custom/vega-lite). I'm working
on getting a better output from this.

With a REPL open in the `lhcb-opendata` namespace you can start to
explore the code. The
[PySpark](https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Spark_HEP_Examples/LHCb_OpenData_Spark.ipynb)
workbook will provide some questions and guidance.


## Apache Spark and Clojure

Clojure is a dynamic language that runs on the JVM and it has
excellent support for Java interop. Spark is a general purpose
distributed data processing engine which is written in Scala but has
APIs in Java, Python and R all of which allow you to process your data
sets with SQL. The
[Sparkling](http://gorillalabs.github.io/sparkling/) is a Clojure
library that has done a lot of the hard work of enabling Clojure data
structures to be used naturally with the Spark Java API. It has also
exposed a lot of the Spark core functionality and some of the Spark
SQL API as Clojure functions. This project uses Sparkling as well as
some additionally libraries exposing the more of the Dataset and
related class APIs. This is done in the `ignition` namespace. Don't be
afraid to check out [Flambo](https://github.com/yieldbot/flambo)
another Clojure project exposing the Spark API.

This project uses the Spark Dataset API to reconstruct the B
candidate's mass and to plot a historgram of the distribution. It
primarily provides examples of how to use the SQL API programmatically
and work with structured datasets. This mostly means calling the API
directly with fairly straight forward conversion of Clojure to Java
data structures. There is a more involved example of using an User
Defined Function (UDF) in the Dataset API to reduce a column to bin
counts. Using a UDF on a column object is a lot easier than writing a
Spark Dataset function to work with `map` as there you must access the
row object yourself and also return a new extended row. I've lost the
example code but it involves a lot more boiler plate.


## Getting Start with Clojure

Head over to [leiningen](https://leiningen.org/) and download and
install the binary. This project has a `project.clj` file which is
understood by lein and will allow it to pull in all the dependencies,
build a JAR or uberjar, and most importantly start a Clojure REPL. If
you're completely new to Clojure I recommend checking out [Clojure for
the Brave and True](https://www.braveclojure.com/) and also installing
Clojure support for your favourite editor.


## Getting Started With BeakerX

My intention was to give this project as a live demo using a notebook
and after some searching found [BeakerX](http://beakerx.com) which
extends Jupyter notebooks for JVM languages. This was a partial
success. I never managed to make it into a clean demo but was able to
use the notebook to run Clojure, Spark and produce some plots. It's a
little scrappy at the moment as I gave up on it to write some slides
instead. I'll try to fix it and tidy it up.

The `docker-compose.yml` file will bring up a BeakerX container, bind
some local directories for the data required in the project and also a
`.m2` directory. This is used as a persistent Maven cache so that the
container doesn't have to download all it's dependencies every time
you run the notebook. Spark and Hadoop are not lightweight JARs. I
have bound the necessary port to access the notebook but also expose
the local network to the container. This was part of a failed
experiment to allow the notebook to connect to a local Spark instance.

    docker-compose up

will get you started. You can install Docker and Docker compose from
[here](https://www.docker.com/) and
[here](https://docs.docker.com/compose/). You can of course install
BeakerX locally but I had some Python 2 vs 3 issues so went down the
container route.
