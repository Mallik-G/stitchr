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

package com.stitchr.core.dbapi

import com.stitchr.util.SharedSession.spark
import com.stitchr.util.database.JdbcProps

import com.databricks.dbutils_v1.DBUtilsHolder.dbutils._
import java.util.Properties

import org.apache.spark.annotation._
import org.apache.spark.sql.DataFrame

case class SparkJdbcImpl(jdbcProps: JdbcProps) extends SparkJdbc {

  def setupUrl(): String =
    s"jdbc:${jdbcProps.dbms}://${jdbcProps.host}:${jdbcProps.port}/${jdbcProps.database}"

  def setupConnectionProperties(): Properties = {
    val connectionProperties = new Properties()
    connectionProperties.put("driver", jdbcProps.driver)

    connectionProperties.put("user", jdbcProps.user)
    connectionProperties.put("password", if (jdbcProps.db_scope == "open") jdbcProps.pwd else secrets.get(scope = jdbcProps.db_scope, key = jdbcProps.pwd))
    connectionProperties.put("sslmode", jdbcProps.sslmode)

    connectionProperties
  }

  override def readDF(query: String): DataFrame =
    // may generalize with a map of key/value pairs
    spark.read
      .option("sslmode", jdbcProps.sslmode) // seems postgres specific?
      .option("fetchsize", jdbcProps.fetchsize)
      .jdbc(url, s"( $query ) as t", connectionProperties)

  /** EXPERIMENTAL
   * ::Experimental::
   * assume a partition_key is wrpaped and generated in the sql.
   * for postgres the hash column is the md5 of the target field  modulo number of partitions such as for
   * example up to 16 partitions in postgres
   * , table=s"(select ('x' || substr(md5(cast($partitionKey as varchar)),1,2))::bit(4)::int as partition_key, q.* from ($query ) q ) as t"
   *
   * @since
   */
  @Experimental
  //@Unstable
  override def readDF(query: String, partitionKey: String = "key", numberOfPartitions: Int = 2): DataFrame =
    if (numberOfPartitions == 1) readDF(query)
    else
      /* spark.read
      .option("sslmode", jdbcProps.sslmode)
      .option("fetchsize", jdbcProps.fetchsize)
      .jdbc(
          url = url,
          table = s"(select mod($partitionKey, $numberOfPartitions) as bucket, q.* from ($query ) q ) as t",
          columnName = "bucket",
          lowerBound = 0L,
          upperBound = numberOfPartitions,
          numPartitions = numberOfPartitions,
          connectionProperties
      ) */
      // attempting to use the full abstracted api from spark... weakness is that options need to be all strings when passed in...
      // we need to figure out functions to translate from strings to integers in general as partitionKey needs to be an integer
      spark.read
        .format("jdbc")
        .options(
            Map(
                "url"             -> url,
                "dbTable"         -> s"(select mod($partitionKey, $numberOfPartitions) as bucket, q.* from ($query ) q ) as t",
                "partitionColumn" -> "bucket",
                "lowerBound"      -> "0",
                "upperBound"      -> numberOfPartitions.toString,
                "numPartitions"   -> numberOfPartitions.toString,
                "driver"          -> jdbcProps.driver,
                "user"            -> jdbcProps.user,
                "password"        -> (if (jdbcProps.db_scope == "open") jdbcProps.pwd else secrets.get(scope = jdbcProps.db_scope, key = jdbcProps.pwd)),
                "fetchsize"       -> jdbcProps.fetchsize.toString,
                "sslmode"         -> jdbcProps.sslmode,
                "id"              -> "0" // it seems spark picks what it needs!!?
            )
        )
        .load()
        .drop("bucket") // this was a hidden side effect

  // to be implemented
  override def writeTable(dataFrame: DataFrame, tableName: String, numberPartitions: Int = 32, saveMode: String): Unit =
    dataFrame.write
      .format("jdbc")
      .mode(saveMode)
      .option("driver", jdbcProps.driver)
      .option("dbTable", tableName)
      .option("url", url)
      .option("user", jdbcProps.user)
      .option("password", (if (jdbcProps.db_scope == "open") jdbcProps.pwd else secrets.get(scope = jdbcProps.db_scope, key = jdbcProps.pwd)))
      .option("numPartitions", numberPartitions.toString)
      .save()

}
