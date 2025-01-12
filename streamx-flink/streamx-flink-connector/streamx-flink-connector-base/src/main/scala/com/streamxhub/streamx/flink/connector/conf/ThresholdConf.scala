/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.conf

import com.streamxhub.streamx.common.util.ConfigUtils
import com.streamxhub.streamx.flink.connector.conf.FailoverStorageType.{FailoverStorageType, Console, NONE, Kafka, MySQL}

import java.util.Properties
import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

case class ThresholdConf(prefixStr: String, parameters: Properties) {

  private val option: ThresholdConfigOption = ThresholdConfigOption(prefixStr, parameters)

  val bufferSize: Int = option.bufferSize.get()
  val queueCapacity: Int = option.queueCapacity.get()
  val delayTime: Long = option.delayTime.get()
  val timeout: Int = option.timeout.get()
  val numWriters: Int = option.numWriters.get()
  val maxRetries: Int = option.maxRetries.get()
  val storageType: FailoverStorageType = option.storageType.get()
  val failoverTable: String = option.failoverTable.get()

  def getFailoverConfig: Properties = {
    storageType match {
      case Console | NONE => null
      case Kafka => ConfigUtils.getConf(parameters.toMap.asJava, "failover.kafka.")
      case MySQL => ConfigUtils.getConf(parameters.toMap.asJava, "failover.mysql.")
      case _ => throw new IllegalArgumentException(s"[StreamX] usage error! failover.storage must not be null! ")
    }
  }
}

object FailoverStorageType extends Enumeration {
  type FailoverStorageType = Value
  val Console, MySQL, Kafka, NONE = Value

  def get(key: String): Value = values.find(_.toString.equalsIgnoreCase(key)).get

}
