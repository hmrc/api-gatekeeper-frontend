/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import uk.gov.hmrc.apiplatform.modules.deskpro.config.DeskproHorizonConfig
import uk.gov.hmrc.apiplatform.modules.deskpro.models.{DeskproCreateOrganisationRequest, DeskproCreatePersonRequest, DeskproPerson}
import play.api.libs.json.JsNull
import play.api.libs.json.JsObject

@Singleton
class DeskproHorizonConnector @Inject() (http: HttpClientV2, config: DeskproHorizonConfig)(implicit val ec: ExecutionContext) {

  def getOrganisations()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.get(url"${config.deskproHorizonUrl}/api/v2/organizations")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[HttpResponse]
  }

  def createOrganisation(name: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http
      .post(url"${config.deskproHorizonUrl}/api/v2/organizations")
      .withProxy
      .withBody(Json.toJson(DeskproCreateOrganisationRequest(name)))
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[HttpResponse]
  }

  def getPeople()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.get(url"${config.deskproHorizonUrl}/api/v2/people")
      .withProxy
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[HttpResponse]
  }

  def createPerson(name: String, email: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http
      .post(url"${config.deskproHorizonUrl}/api/v2/people")
      .withProxy
      .withBody(Json.toJson(DeskproCreatePersonRequest(name, email)))
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[HttpResponse]
  }

  def addMembership(orgId: Int, personId: Int)(implicit hc: HeaderCarrier): Future[HttpResponse]  = {
    http
      .post(url"${config.deskproHorizonUrl}/api/v2/organizations/$orgId/members")
      .setHeader(
        CONTENT_TYPE -> "application/x-www-form-urlencoded",
        AUTHORIZATION -> config.deskproHorizonApiKey
      )
      .withProxy
      .withBody(
        Map(
          "person" -> Seq(personId.toString)
        )
      )
      .execute[HttpResponse]
  }

  def getPerson(email: String)(implicit hc: HeaderCarrier): Future[Option[DeskproPerson]] = {
    http
      .get(url"${config.deskproHorizonUrl}/api/v2/people?emails=$email")
      .setHeader(AUTHORIZATION -> config.deskproHorizonApiKey)
      .execute[HttpResponse]
      .map { response =>
        response.json("data")(0) match {
          case data: JsObject => Some(data.as[DeskproPerson])
          case _ => None
        }
      }
  }
}
