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

package org.apache.spark.ui.jobs

import javax.servlet.http.HttpServletRequest

import scala.xml.Node

import org.apache.spark.status.PoolData
import org.apache.spark.status.api.v1._
import org.apache.spark.ui.{UIUtils, WebUIPage}

/** Page showing specific pool details */
private[ui] class PoolPage(parent: StagesTab) extends WebUIPage("pool") {

  def render(request: HttpServletRequest): Seq[Node] = {
    // stripXSS is called first to remove suspicious characters used in XSS attacks
    val poolName = Option(UIUtils.stripXSS(request.getParameter("poolname"))).map { poolname =>
      UIUtils.decodeURLParameter(poolname)
    }.getOrElse {
      throw new IllegalArgumentException(s"Missing poolname parameter")
    }

    // For now, pool information is only accessible in live UIs
    val pool = parent.sc.flatMap(_.getPoolForName(poolName)).getOrElse {
      throw new IllegalArgumentException(s"Unknown pool: $poolName")
    }

    val uiPool = parent.store.asOption(parent.store.pool(poolName)).getOrElse(
      new PoolData(poolName, Set()))
    val activeStages = uiPool.stageIds.toSeq.map(parent.store.lastStageAttempt(_))
    val activeStagesTable =
      new StageTableBase(parent.store, request, activeStages, "", "activeStage", parent.basePath,
        "stages/pool", parent.isFairScheduler, parent.killEnabled, false)

    val poolTable = new PoolTable(Map(pool -> uiPool), parent)
    var content = <h4>Summary </h4> ++ poolTable.toNodeSeq
    if (activeStages.nonEmpty) {
      content ++= <h4>Active Stages ({activeStages.size})</h4> ++ activeStagesTable.toNodeSeq
    }

    UIUtils.headerSparkPage("Fair Scheduler Pool: " + poolName, content, parent)
  }
}
