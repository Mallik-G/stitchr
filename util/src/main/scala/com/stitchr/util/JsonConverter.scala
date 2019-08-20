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

package com.stitchr.util

import net.liftweb.json.Serialization.write
import net.liftweb.json._

object JsonConverter {
  def toJson[T](t: T): String = {
    implicit val formats = net.liftweb.json.DefaultFormats
    write[T](t)
  }

  def fromJson[T](j: String)(implicit m: Manifest[T]): T = {
    implicit val formats = net.liftweb.json.DefaultFormats
    parse(j).extract[T]
  }
}