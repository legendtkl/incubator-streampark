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

package com.streamxhub.streamx.flink.connector.mongo.source

import com.mongodb.client.{FindIterable, MongoCollection, MongoCursor}
import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.connector.mongo.internal.MongoSourceFunction
import com.streamxhub.streamx.flink.core.scala.StreamingContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.DataStream
import org.bson.Document

import java.util.Properties
import scala.annotation.meta.param


object MongoSource {

  def apply(@(transient@param) property: Properties = new Properties())(implicit ctx: StreamingContext): MongoSource = new MongoSource(ctx, property)

}

class MongoSource(@(transient@param) val ctx: StreamingContext, property: Properties = new Properties()) {


  /**
   *
   * @param queryFun
   * @param resultFun
   * @param prop
   * @tparam R
   * @return
   */

  def getDataStream[R: TypeInformation](
                                         collection: String,
                                         queryFun: (R, MongoCollection[Document]) => FindIterable[Document],
                                         resultFun: MongoCursor[Document] => List[R],
                                         running: Unit => Boolean)(implicit prop: Properties = new Properties()): DataStream[R] = {

    Utils.copyProperties(property, prop)
    val mongoFun = new MongoSourceFunction[R](collection, prop, queryFun, resultFun, running)
    ctx.addSource(mongoFun)

  }

}



