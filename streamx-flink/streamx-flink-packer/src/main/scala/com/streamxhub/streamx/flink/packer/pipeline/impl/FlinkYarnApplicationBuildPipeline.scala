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

package com.streamxhub.streamx.flink.packer.pipeline.impl

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.enums.DevelopmentMode
import com.streamxhub.streamx.common.fs.{FsOperator, HdfsOperator, LfsOperator}
import com.streamxhub.streamx.common.util.Utils
import com.streamxhub.streamx.flink.packer.maven.MavenTool
import com.streamxhub.streamx.flink.packer.pipeline._
import org.apache.commons.codec.digest.DigestUtils

import java.io.{File, FileInputStream, IOException}

/**
 * Building pipeline for flink yarn application mode
 *
 * @author benjobs
 */
class FlinkYarnApplicationBuildPipeline(request: FlinkYarnApplicationBuildRequest) extends BuildPipeline {

  /**
   * the type of pipeline
   */
  override def pipeType: PipelineType = PipelineType.FLINK_YARN_APPLICATION

  override def offerBuildParam: FlinkYarnApplicationBuildRequest = request

  /**
   * the actual build process.
   * the effective steps progress should be implemented in
   * multiple BuildPipeline.execStep() functions.
   */
  @throws[Throwable] override protected def buildProcess(): SimpleBuildResponse = {
    execStep(1) {
      request.developmentMode match {
        case DevelopmentMode.FLINKSQL =>
          LfsOperator.mkCleanDirs(request.localWorkspace)
          HdfsOperator.mkCleanDirs(request.yarnProvidedPath)
        case _ =>
      }
      logInfo(s"recreate building workspace: ${request.yarnProvidedPath}")
    }.getOrElse(throw getError.exception)

    val mavenJars =
      execStep(2) {
        request.developmentMode match {
          case DevelopmentMode.FLINKSQL =>
            val mavenArts = MavenTool.resolveArtifacts(request.dependencyInfo.mavenArts)
            mavenArts.map(_.getAbsolutePath) ++ request.dependencyInfo.extJarLibs
          case _ => Set[String]()
        }
      }.getOrElse(throw getError.exception)

    execStep(3) {
      mavenJars.foreach(jar => {
        uploadToHdfs(FsOperator.lfs, jar, request.localWorkspace)
        uploadToHdfs(FsOperator.hdfs, jar, request.yarnProvidedPath)
      })
    }.getOrElse(throw getError.exception)

    SimpleBuildResponse()
  }

  @throws[IOException] private[this] def uploadToHdfs(fsOperator: FsOperator, origin: String, target: String): Unit = {
    val originFile = new File(origin)
    if (!fsOperator.exists(target)) {
      fsOperator.mkdirs(target)
    }
    if (originFile.isFile) {
      // check file in upload dir
      fsOperator match {
        case FsOperator.lfs =>
          fsOperator.copy(originFile.getAbsolutePath, target)
        case FsOperator.hdfs =>
          val uploadFile = s"${Workspace.remote.APP_UPLOADS}/${originFile.getName}"
          if (fsOperator.exists(uploadFile)) {
            Utils.tryWithResource(new FileInputStream(originFile))(inputStream => {
              if (DigestUtils.md5Hex(inputStream) != fsOperator.fileMd5(uploadFile)) {
                fsOperator.upload(originFile.getAbsolutePath, uploadFile)
              }
            })
          } else {
            fsOperator.upload(originFile.getAbsolutePath, uploadFile)
          }
          // copy jar from upload dir to target dir
          fsOperator.copy(uploadFile, target)
      }
    } else {
      fsOperator match {
        case FsOperator.hdfs => fsOperator.upload(originFile.getAbsolutePath, target)
        case _ =>
      }
    }
  }

}

object FlinkYarnApplicationBuildPipeline {
  def of(request: FlinkYarnApplicationBuildRequest): FlinkYarnApplicationBuildPipeline = new FlinkYarnApplicationBuildPipeline(request)
}
