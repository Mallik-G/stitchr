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

package com.stitchr.core.util

import com.stitchr.core.common.Encoders.{ Column, DataSource }
import com.stitchr.sparkutil.SharedSession.spark
import com.stitchr.core.dbapi.PostgresDialect

import org.apache.spark.sql.{ DataFrame, Row }
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.execution.datasources.jdbc.JdbcUtils.getSchema

import java.sql.{ ResultSet, ResultSetMetaData, SQLException }

// import com.stitchr.core.common.Encoders.JdbcProps
import com.stitchr.util.database.JdbcProps
import com.typesafe.config.Config

import scala.collection.mutable.ArrayBuffer

object Convert {
  /* pull out for now  as it is not used anymore
  // case class JdbcProps(dbms: String, driver: String, host: String, port: Int, database: String, user: String = null, pwd: String = null, fetchsize: Int = 10000)
 def fromRow2JdbcProp(r: Row): JdbcProps =
   //for user/pwd we will use the sys env for now
   JdbcProps(r.get(1).toString, r.get(2).toString, r.get(3).toString, r.get(4).asInstanceOf[Int], r.get(5).toString, null, null)

  def row2JdbcProp(r: Row): JdbcProps =
    //for user/pwd we will use the sys env for now
    JdbcProps(r.get(1).toString, r.get(2).toString, r.get(3).toString, r.get(4).asInstanceOf[Int], r.get(5).toString, null, null)
   */

  /**
   * used for testing of the metadata code for now
   */
  // we use a dbIndex to specify general data source jdbc or use it for teh data catalog reference
  def config2JdbcProp(c: Config, dbIndex: String = "jdbc"): JdbcProps =
    JdbcProps(
        c.getString(s"$dbIndex.dbengine"),
        c.getString(s"$dbIndex.driver"),
        c.getString(s"$dbIndex.host"),
        c.getInt(s"$dbIndex.port"),
        c.getString(s"$dbIndex.db"),
        dbIndex,
        c.getString(s"$dbIndex.user"),
        c.getString(s"$dbIndex.pwd")
    )

  def dataSourceNode2JdbcProp(dataSource: DataSource): JdbcProps =
    // here we retrieve the url (jdbc connection or other) for the datasource
    JdbcProps(
        dataSource.storage_type,
        dataSource.driver,
        dataSource.host,
        dataSource.port,
        dataSource.database,
        "jdbc",
        dataSource.user,
        dataSource.pwd,
        dataSource.fetchsize
    )

  /**
   * warning the following converter are applied on result sets and are memory intensive as they generate lists and use memory linearly with the resultset cardinality
   * So they are useful for small sets associated with metadata and catalog information
   */
  // not used ... yet
  @throws[SQLException]
  def resultSetMeta2List(rsmd: ResultSetMetaData): List[Column] =
    try {
      val columnCount = rsmd.getColumnCount
      val columns = new ArrayBuffer[Column]
      var i = 1
      while (i <= columnCount) {
        columns += Column(i, rsmd.getColumnName(i), rsmd.getColumnTypeName(i))
        i += 1
      }
      columns.toList
    } finally {
      // rsmd
    }

  // EXPERIMENTAL and incomplete. need to pull the hard-coded reference to the PostgresDialect
  @throws[SQLException]
  def resultSet2List(rs: ResultSet): (ArrayBuffer[Row], StructType, Int) =
    try {
      val rows = new ArrayBuffer[Row]
      val rsMetadata: ResultSetMetaData = rs.getMetaData
      val columnCount = rsMetadata.getColumnCount
      //val rsmdl = resultSetMeta2List(rsMetadata)
      println("table metadata schema")
      var j = 1
      while ({ j <= rsMetadata.getColumnCount }) {
        println("Column Name is: " + rsMetadata.getColumnName(j))
        println("Column Type is: " + rsMetadata.getColumnTypeName(j))
        // Do stuff with name
        j += 1
      }
      // schema is position|       name|att_type|
      //val a = rsmdl.map(r => (r.name, r.att_type)).toArray
      // val schema = generateStructSchema(a)

      // Prefer Spark's getSchema function
      // issue is to find the dialect or use a default as a catch all (maybe write a default one or just refer to the PostgresDialect always...
      // need to use the jdbc prefix to match to the registred dialect!! this is undocumented. this would be added ot the datasource metadata :-(
      // or use my own override of the dialect... which is not recommended val schema: StructType = getSchema(rs, PostgresDialect, alwaysNullable = true)
      // val schema: StructType = getSchema(rs, JdbcDialects.get("jdbc:postgresql"), alwaysNullable = true)
      val schema: StructType = getSchema(rs, new PostgresDialect) // ??, alwaysNullable = true)

      while ({ rs.next }) {
        val row = new ArrayBuffer[AnyRef]
        var i = 1
        while (i <= columnCount) {
          row += rs.getObject(i)
          i += 1
        }
        val r = Row.fromSeq(row)
        rows += r
      }
      (rows, schema, columnCount)
    } finally {
      // rs.close() // not doing it as I may need to rewind?!
    }

  def resultSet2DataFrame(rs: ResultSet): DataFrame = {
    val (r, sc, _) = resultSet2List(rs)
    spark.createDataFrame(spark.sparkContext.parallelize(r), sc) // we run parallelize to create an RDD[Row]
  }

}
