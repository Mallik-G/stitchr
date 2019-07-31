/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// spark-shell --jars $STITCHR_ROOT/app/target/stitchr-app-0.1-SNAPSHOT-jar-with-dependencies.jar

/**
 * Used to demo use cases
 * 1. Source and Target are a database
 * 2. Source and Target are Spark files and Spark warehouse
 *
 * The data is based on tpcds.
 * data files are stripped from the last | to make them directly readable and are stored under demo/data/tpcds
 * generate the data using tpcds tools and place in that directory before the demo.
 * You can find a copy ready to use under nhachem/stitchr-demo
 */
package com.stitchr.app

import com.stitchr.app.Derivation.{args, run}
import com.stitchr.core.api.DataSet.Implicits
import com.stitchr.core.dbapi.SparkJdbcImpl
import com.stitchr.core.registry.RegistrySchema.dataSourceDF
import com.stitchr.core.registry.RegistryService.{getDataSource, getDataset, getObjectRef}
import com.stitchr.core.util.Convert.dataSourceNode2JdbcProp
import com.stitchr.sparkutil.SharedSession.spark
import com.stitchr.util.Logging
import com.stitchr.util.Properties.configS3
import com.stitchr.util.Util.time

object DataIngestService {

  /**
   * expected parameters are
   * List[String] as a list of query identifiers. It assumes that those were already derived and abstracted in the spark database. in other wor=ds the local db has been instantiated
   * and
   * target type: file or database
   */
  /**
   * edit the parameters below to go against a target dbms or files. By default we run q2 on files (on yr laptop)
   */
  def instantiateQueryList(ql: List[String]): Unit = { // , st: String = "file"): Unit = {

    val _ = configS3() // needed for AWS ... may make conditional based on config or metadata

    // assumed to be not needed
    // val ds = new DerivationService
    // ds.deriveQueryList(ql)

    // Then run the actual materialization of the target objects
    import spark.implicits._
    ql.foldLeft()(
        (_, next) => {
          println(s"instantiating the target for $next")
          val (viewName, dfm) = getDataset(getObjectRef(next)).materialize
          println(s"viewname is $viewName")
          spark.sparkContext.setLogLevel("INFO")
          time(dfm.count, "counting the rows in the materialized object")
          dfm.printSchema()

        }
    )
  }
}

/*
may replace the DataIngestService (or be merged with it)
 */
object IngestDataSet extends App {

  def load2DataLake(ql: List[String]): Unit = { // , st: String = "file"): Unit = {

    val _ = configS3() // needed for AWS ... will have to extend to include GS amd  make conditional based on config or metadata

    // instantiate the derived views
    ql.foldLeft()(
      (_, next) => {
        println(s"loading to data lake $next") // for storage_type $st")
        getDataset(next).move2Lake

      }
    )
    // show changes to catalog as we iterate.. will pull out
    spark.catalog.listTables.show(50, false)
    logging.log.info(s"number of table in the inSessionDB is ${spark.catalog.listTables.count()}")
  }

  val _ = configS3() // needed for AWS ... will have to extend to include GS amd  make conditional based on config or metadata

  val logging = new Logging

  spark.sparkContext.setLogLevel("INFO")

  // just list the session info
  val configMap:Map[String, String] = spark.conf.getAll
  logging.log.info(s"configMap is $configMap")


  spark.catalog.listTables.show(50, false)
  val usage =
    """
    Usage: Derivation [commaDelimitedObjectRef] ]
    """
  // expect 2 arguments. first is a list of object references and the second is a storage type...
  // we may better doing it as a list of list and decipher... but for now it is fine
  if (args.length < 1) println(usage)
  else {

    val ql = args(0).toString.split(",").toList
    println(s"list of queries is $ql")
    load2DataLake(ql)
  }

}