/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice._
import uk.gov.hmrc.gatekeeper.models._
import play.api.http.Status
import play.api.libs.json.Json

trait DeveloperServiceStub {
  val emailPreferencesUrl          = "/developers/email-preferences"
  val emailPreferencesPaginatedUrl = "/developers/email-preferences-paginated"
  val allUrl                       = "/developers/all"
  val allUrlPaginated              = "/developers/all-paginated?offset=0&limit=15"
  val byEmails                     = "/developers/get-by-emails"

  def primeDeveloperServiceAllSuccessWithUsers(users: Seq[RegisteredUser]): Unit = {

    stubFor(get(urlEqualTo(allUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())
      ))
  }

  def primeDeveloperServiceAllSuccessWithUsersPaginated(totalCount: Int, users: Seq[RegisteredUser]): Unit = {

    stubFor(get(urlEqualTo(allUrlPaginated))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(UserPaginatedResponse(totalCount, users.toList)).toString())
      ))
  }

  def primeDeveloperServiceGetByEmails(users: Seq[RegisteredUser]): Unit = {

    stubFor(post(urlEqualTo(byEmails))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())
      ))
  }

  def primeDeveloperServiceEmailPreferencesByTopic(users: Seq[RegisteredUser], topic: TopicOptionChoice): Unit = {
    val emailpreferencesByTopicUrl = emailPreferencesUrl + "?topic=" + topic.toString
    stubFor(get(urlEqualTo(emailpreferencesByTopicUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())
      ))
  }

  def primeDeveloperServiceEmailPreferencesByTopicAndCategory(users: Seq[RegisteredUser], topic: TopicOptionChoice, category: APICategoryDetails): Unit = {
    val emailpreferencesByTopicAndCategoryUrl = emailPreferencesUrl + "?topic=" + topic.toString + "&regime=" + category.category
    stubFor(get(urlEqualTo(emailpreferencesByTopicAndCategoryUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())
      ))
  }

  def primeDeveloperServiceEmailPreferencesBySelectedAPisTopicAndCategory(users: Seq[RegisteredUser], selectedApis: Seq[ApiDefinitionGK], topic: TopicOptionChoice): Unit = {
    val categories: Seq[APICategory] = selectedApis.map(_.categories.getOrElse(Seq.empty)).reduce(_ ++ _).distinct

    val topicParam    = s"topic=${topic.toString}"
    val regimeParams  = categories.map(category => s"&regime=${category.value}").mkString
    val serviceParams = selectedApis.map(api => s"&service=${api.serviceName}").mkString

    val emailpreferencesByTopicAndCategoryUrl = s"$emailPreferencesUrl?$topicParam$regimeParams$serviceParams"
    stubFor(get(urlEqualTo(emailpreferencesByTopicAndCategoryUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())
      ))
  }

  def primeDeveloperServiceEmailPreferencesBySelectedTopicPaginated(users: Seq[RegisteredUser], topic: TopicOptionChoice, offset: Int, limit: Int): Unit = {
    val topicParam       = s"topic=${topic.toString}"
    val paginationParams = s"offset=$offset&limit=$limit"

    val emailPreferencesByTopicUrl = s"$emailPreferencesPaginatedUrl?$topicParam&$paginationParams"
    stubFor(get(urlEqualTo(emailPreferencesByTopicUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(UserPaginatedResponse(users.size, users.toList)).toString())
      ))
  }

  def primeDeveloperServiceEmailPreferencesBySelectedSubscribedApisPaginated(users: Seq[RegisteredUser], service: Seq[String], offset: Int, limit: Int): Unit = {
    val serviceParam     = s"service=${service.head}"
    val paginationParams = s"offset=$offset&limit=$limit"

    val emailPreferencesBySelectedSubscribedApiUrl = s"$emailPreferencesPaginatedUrl?$serviceParam&$paginationParams"
    stubFor(get(urlEqualTo(emailPreferencesBySelectedSubscribedApiUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(UserPaginatedResponse(users.size, users.toList)).toString())
      ))
  }

  def primeDeveloperServiceEmailPreferencesBySelectedUserTaxRegimePaginated(users: Seq[RegisteredUser], regimes: Seq[String], offset: Int, limit: Int): Unit = {
    val regimeParam      = s"regime=${regimes.head}"
    val paginationParams = s"offset=$offset&limit=$limit"

    val emailPreferencesBySelectedUserTaxRegimeUrl = s"$emailPreferencesPaginatedUrl?$regimeParam&$paginationParams"
    stubFor(get(urlEqualTo(emailPreferencesBySelectedUserTaxRegimeUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(UserPaginatedResponse(users.size, users.toList)).toString())
      ))
  }
}
