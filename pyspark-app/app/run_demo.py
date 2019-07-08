#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""
note that the variables are local to ex
so to run a new derivation one would need to use run_demo.s....

to run interactively using pyspark:
in the directory where the run_demo.py is located run

pyspark --jars $STITCHR_ROOT/core/target/stitchr-core-0.1-SNAPSHOT-jar-with-dependencies.jar

then at the prompt type

import run_demo

"""
import pyspark
from pyspark.sql import SparkSession

import os
import sys

""" setting up the path to include Stitchr and other project related imports"""
sys.path.append(os.environ['STITCHR_ROOT'] + '/pyspark-app/app')

from Stitchr import *

spark = (pyspark.sql.SparkSession.builder.getOrCreate())
spark.sparkContext.setLogLevel('WARN')

print("Spark Version:", spark.sparkContext.version)

s = Stitchr(spark, spark.sparkContext)

"""
note that the calls return a spark session. Which is the scala spark session. It is shared between the same calls on the stitchr class
as well as if we invoke different oinstances of Stitchr 

"""

ss2 = s.derive_query('q2', 'file')

sc2 = ss2.catalog.container.sqlContext()
sc2.tables().show()
ss2.catalog.container.table("q2").show(False)

ss4 = s.derive_query('q4', 'file')

sc4 = ss4.catalog.container.sqlContext()

sc4.tables().show()

ss2.catalog.container.table("q4").show(False)
sc2.tables().show()

ss4.catalog.container.table("q2").show(False)
## envirornment that  we can work with ss2 or ss4 ... as those are containers to the spark session on the jvm side
sc4.tables().show()

ss4.catalog.container.table("q4").show(False)

ss2.catalog.container.table("q4").show(False)
sc2.tables().show()

## new instance of Stitchr  ut shares same session

s1 = Stitchr(spark, spark.sparkContext)
ss = s1.derive_query('q2', 'file')

sc = ss.catalog.container.sqlContext()
sc.tables().show()
ss.catalog.container.table("q2").show(False)