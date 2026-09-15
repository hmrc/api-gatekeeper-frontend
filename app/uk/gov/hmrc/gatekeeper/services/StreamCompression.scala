/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.services

import java.time.Instant
import scala.collection.mutable.{ArrayBuffer, ListBuffer, Map}

import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApiIdentifier, ApplicationId}
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector.AppWithSubscriptionsForCsvResponse

sealed trait Output

case class OutputApp[A](
    app: A,
    subscriptions: Option[Set[Int]] = None
  ) extends Output

case class OutputSubscription(apiIdentifier: ApiIdentifier) extends Output

object OutputApp {
  def writes[A](implicit w: OWrites[A]): OWrites[OutputApp[A]] = Json.writes[OutputApp[A]]
  def reads[A](implicit r: Reads[A]): Reads[OutputApp[A]]      = Json.reads[OutputApp[A]]
}

object OutputSubscription {
  implicit val fmt: Format[OutputSubscription] = Json.valueFormat[OutputSubscription]
}

object Output {

  def writes[A](implicit aWrites: OWrites[A]): OWrites[Output] = new OWrites[Output] {
    val x: OWrites[ApiIdentifier] = implicitly
    val y: OWrites[OutputApp[A]]  = OutputApp.writes[A]

    def writes(o: Output): JsObject = o match {
      case OutputSubscription(apiIdentifier) => x.writes(apiIdentifier)
      case oa: OutputApp[A]                  => y.writes(oa)
    }
  }

  def reads[A](implicit aReads: Reads[A]): Reads[Output] = new Reads[Output] {
    val x: Reads[ApiIdentifier] = implicitly
    val y: Reads[OutputApp[A]]  = OutputApp.reads[A]

    override def reads(json: JsValue): JsResult[Output] = {
      x.reads(json).map(OutputSubscription.apply).orElse(
        y.reads(json)
      )
    }
  }
}

case class SimpleApp(
    id: ApplicationId,
    name: ApplicationName,
    createdOn: Instant,
    lastAccess: Instant
  )

object SimpleApp {
  implicit val fmt: OFormat[SimpleApp] = Json.format[SimpleApp]
}

object StreamCompression {
  type LookupTable = Map[ApiIdentifier, Int]

  def decompressStream(in: List[Output]): List[AppWithSubscriptionsForCsvResponse] = {
    val buf = ArrayBuffer.empty[ApiIdentifier]

    decompress(buf, in)._2
  }

  type fn = (ArrayBuffer[ApiIdentifier], List[Output]) => (ArrayBuffer[ApiIdentifier], List[AppWithSubscriptionsForCsvResponse])

  private val decompress: fn = (lookupTable, outputs) => {
    val resultList = ArrayBuffer.empty[AppWithSubscriptionsForCsvResponse]

    outputs.foreach(_ match {
      case OutputSubscription(id)                                         => lookupTable.append(id)
      case OutputApp(SimpleApp(id, name, createdOn, lastAccess), subKeys) =>
        val subs: Option[Set[ApiIdentifier]] = subKeys.map(_.map(k => lookupTable.apply(k - 1)))
        val oLastAccess                      = Some(lastAccess).filterNot(_.getEpochSecond() == createdOn.getEpochSecond())
        resultList.append(AppWithSubscriptionsForCsvResponse(id, name, oLastAccess, subs.getOrElse(Set.empty)))
    })

    (lookupTable, resultList.toList)
  }
}
