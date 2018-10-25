# Working Through an LHCb Open Data Set in Clojure and Spark

The following `README.md` is a bit of a brain dump. I'll hopefully
make it more coherent with time. For now check me out, start a Clojure
REPL and have a play around in the `lhcb-opendata`
space. Alternatively use `docker-compose up` to bring up the BeakerX
notebook, fix it and get going. It's missing a function.

This is an example project I created for a talk I gave a [Voxx Days
Bristol 2018](https://voxxeddays.com/bristol/)
([Me](https://vxdbristol2018.confinabox.com/talk/YKT-9819/Putting_the_Spark_in_Functional_Fashion_Tech_Analytics)). It
is an example of some data processing using the
[Clojure](https://clojure.org) programming language to build an
[Apache Spark](https://apache.spark.org) job.

I needed an open data set and also something to do with that data set
for this experiment and wasn't confident I could come up with a good
one myself. I found the
[LHCb](http://lhcb-public.web.cern.ch/lhcb-public/) open data
[project](https://github.com/lhcb/opendata-project) done in PySpark
[here](https://github.com/LucaCanali/Miscellaneous/blob/master/Spark_Notes/Spark_HEP_Examples/LHCb_OpenData_Spark.ipynb)
and was inspired. Mostly because I did my PhD on LHCb from 2006 to 2011.

This project is a work in progress and not a neatly packaged
runnable. Hopefully I'll delete this line. It's also not a physics nor
LHCb analysis tutorial, you should go to the
[source](https://github.com/lhcb/opendata-project/blob/master/LHCb_Open_Data_Project.ipynb)
for that.

What this project is an example of how to use the excellent
[Sparkling](http://gorillalabs.github.io/sparkling/) library to
construct your Spark code in Clojure. Don't be afraid to check out
[Flambo](https://github.com/yieldbot/flambo). The code in the project
is primarily using the Spark Dataset API and it's programmatic
construction of SQL like statements. There is also an example of how
to create a User Definited Function (UDF) in Clojure.

I have found Sparkling to be a little out of data with regard to the
Dataset API and so there is a set of namespaces called `ignition`
which expose the Spark Java API handling some of the type conversions
from Clojure to Java. It's building on the Sparkling work and I should
really figure out to bring it inline with their style and make it a
PR.

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

## Getting Started with the LHCb Open Data

You will need to download the data sets
 * PhaseSpaceSimulation.root: http://opendata.cern.ch/eos/opendata/lhcb/AntimatterMatters2017/data/PhaseSpaceSimulation.root)
 * B2HHH_MagnetDown: http://opendata.cern.ch/eos/opendata/lhcb/AntimatterMatters2017/data/B2HHH_MagnetDown.root
 * B2HHH_MagnetUp: http://opendata.cern.ch/eos/opendata/lhcb/AntimatterMatters2017/data/B2HHH_MagnetUp.root

and make sure they're available to the project. Then fire up a REPL
and begin playing. I'll try to create some more obvious entry points
and make the workbook flow better.
