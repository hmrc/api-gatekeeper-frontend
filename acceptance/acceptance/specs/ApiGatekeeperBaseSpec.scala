/*
 * Copyright 2020 HM Revenue & Customs
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

package acceptance.specs


import acceptance.matchers.CustomMatchers
import acceptance.testdata.{AllSubscribeableApisTestData, ApiDefinitionTestData}
import acceptance.pages.{ApplicationsPage, DashboardPage}
import acceptance.{BaseSpec, SignInSugar, WebPage}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.{GivenWhenThen, Matchers}
import play.api.http.Status._
import play.api.libs.json.Json

import scala.io.Source
import connectors.DeveloperConnector.{FindUserIdRequest, FindUserIdResponse}
import model.UserId
import model.RegisteredUser
import play.api.libs.json.Reads
import model.Collaborator
import model.CollaboratorRole
import utils.UserIdTracker


class ApiGatekeeperBaseSpec 
    extends BaseSpec 
    with SignInSugar 
    with Matchers 
    with CustomMatchers 
    with GivenWhenThen
    with AllSubscribeableApisTestData 
    with ApiDefinitionTestData 
    with utils.UrlEncoding
    with UserIdTracker {

  def stubNewApplication(application: String, appId: String) = {
    stubFor(get(urlEqualTo(s"/applications/$appId")).willReturn(aResponse().withBody(application).withStatus(OK)))
  }

  def stubStateHistory(stateHistory: String, appId: String) = {
    stubFor(get(urlEqualTo(s"/gatekeeper/application/$appId/stateHistory")).willReturn(aResponse().withBody(stateHistory).withStatus(OK)))
  }

  def stubApiDefintionsForApplication(apiDefinitions: String, appId: String) = {
    stubFor(get(urlEqualTo(s"/api-definitions?applicationId=$appId&restricted=false")).willReturn(aResponse().withBody(apiDefinitions).withStatus(OK)))
  }

  def stubDevelopers(developers: List[RegisteredUser]) = {
    stubFor(get(urlMatching(s"/developers")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
    stubFor(post(urlMatching(s"/developers/get-by-emails")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
  }

  def stubApplication(application: String, developers: List[RegisteredUser], stateHistory: String, appId: String) = {
    stubNewApplication(application, appId)
    stubStateHistory(stateHistory, appId)
    stubApiDefintionsForApplication(allSubscribeableApis, appId)
    stubDevelopers(developers)
    
    stubGetDeveloper(developers.head.email, Json.stringify(Json.toJson(developers.head)))

  }

import model.{Application, ApplicationId, ClientId, Access, ApplicationState, RateLimitTier, CheckInformation, IpAllowlist}
import model.RateLimitTier.RateLimitTier

import CollaboratorRole.CollaboratorRole
import org.joda.time.DateTime
  

case class MyCollaborator(emailAddress: String, role: CollaboratorRole, userId: UserId)
case class MyApplicationResponse(id: ApplicationId,
                               clientId: ClientId,
                               gatewayId: String,
                               name: String,
                               deployedTo: String,
                               description: Option[String] = None,
                               collaborators: Set[MyCollaborator],
                               createdOn: DateTime,
                               lastAccess: DateTime,
                               access: Access,
                               state: ApplicationState,
                               rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
                               termsAndConditionsUrl: Option[String] = None,
                               privacyPolicyUrl: Option[String] = None,
                               checkInformation: Option[CheckInformation] = None,
                               blocked: Boolean = false,
                               ipAllowlist: IpAllowlist = IpAllowlist())

  case class MyPaginatedApplicationResponse(applications: Seq[MyApplicationResponse], page: Int, pageSize: Int, total: Int, matching: Int)

  def stubApplicationsList() = {
    val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")
    // import play.api.libs.json._ 
    // import play.api.libs.functional.syntax._   

    // def build(emailAddress: String, role: CollaboratorRole): MyCollaborator = {
    //   MyCollaborator(emailAddress, role, idOf(emailAddress))
    // }

    // implicit val collabReads: Reads[MyCollaborator] = (
    //   (JsPath \ "emailAddress").read[String] and
    //   (JsPath \ "role").read[CollaboratorRole]
    // )(build _)

    // implicit val collabWrites = Json.writes[MyCollaborator]

    // import model._
    // import model.RateLimitTier.RateLimitTier
    // import play.api.libs.json.JodaReads._
    // import play.api.libs.json.JodaWrites.JodaDateTimeNumberWrites

    // implicit val xxx = JodaDateTimeNumberWrites

    // implicit val format4 = Json.format[ApplicationState]

    // import ApplicationResponse._

    // implicit val reads1: Reads[MyApplicationResponse] = (
    // (JsPath \ "id").read[ApplicationId] and
    //   (JsPath \ "clientId").read[ClientId] and
    //   (JsPath \ "gatewayId").read[String] and
    //   (JsPath \ "name").read[String] and
    //   (JsPath \ "deployedTo").read[String] and
    //   (JsPath \ "description").readNullable[String] and
    //   (JsPath \ "collaborators").read[Set[MyCollaborator]] and
    //   (JsPath \ "createdOn").read[DateTime] and
    //   (JsPath \ "lastAccess").read[DateTime] and
    //   (JsPath \ "access").read[Access] and
    //   (JsPath \ "state").read[ApplicationState] and
    //   (JsPath \ "rateLimitTier").read[RateLimitTier] and
    //   (JsPath \ "termsAndConditionsUrl").readNullable[String] and
    //   (JsPath \ "privacyAndPolicyUrl").readNullable[String] and
    //   (JsPath \ "checkInformation").readNullable[CheckInformation] and
    //   ((JsPath \ "blocked").read[Boolean] or Reads.pure(false)) and
    //   (JsPath \ "ipAllowlist").read[IpAllowlist]
    // ) (MyApplicationResponse.apply _)

    // implicit val writes1: OWrites[MyApplicationResponse] = Json.writes[MyApplicationResponse]

    // implicit val format2 = Json.format[MyPaginatedApplicationResponse]

    // // val response = Json.parse(paginatedApplications).as[MyPaginatedApplicationResponse]
    // val response = Json.parse(applicationsList).as[List[MyApplicationResponse]]

    // println("----------------")
    // println("----------------")
    // println(Json.prettyPrint(Json.toJson(response)))
    // println("----------------")
    // println("----------------")
    // throw new RuntimeException()

    applicationsList
  }
  def stubPaginatedApplicationList() = {
    val paginatedApplications = Source.fromURL(getClass.getResource("/paginated-applications.json")).mkString.replaceAll("\n", "")
    

    stubFor(get(urlMatching("/applications\\?page.*")).willReturn(aResponse().withBody(paginatedApplications).withStatus(OK)))  
  }

  protected def stubGetDeveloper(email: String, userJsonText: String, userId: UserId = UserId.random) = {
    val requestJson = Json.stringify(Json.toJson(FindUserIdRequest(email)))
    implicit val format = Json.writes[FindUserIdResponse]
    val responseJson = Json.stringify(Json.toJson(FindUserIdResponse(userId)))

    stubFor(post(urlEqualTo("/developers/find-user-id"))
      .willReturn(aResponse().withStatus(OK).withBody(responseJson)))

    stubFor(
      get(urlPathEqualTo("/developer"))
      .withRequestBody(equalToJson(requestJson))
      .withQueryParam("developerId", equalTo(encode(userId.value.toString)))
      .willReturn(
        aResponse().withStatus(OK).withBody(userJsonText)
      )
    )

    // Where we still need the old email route
    // TODO - remove this on completion of APIS-4925
    stubFor(
      get(urlPathEqualTo("/developer"))
      .withQueryParam("developerId", equalTo(email))
      .willReturn(
        aResponse().withStatus(OK).withBody(userJsonText)
      )
    )
  }
  
  def stubApiDefinition() = {
    stubFor(get(urlEqualTo("/api-definition")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
    stubFor(get(urlEqualTo("/api-definition?type=private")).willReturn(aResponse().withStatus(OK).withBody(apiDefinition)))
  }

  def navigateToApplicationPageAsAdminFor(applicationName: String, page: WebPage, developers: List[RegisteredUser]) = {
    Given("I have successfully logged in to the API Gatekeeper")
    stubPaginatedApplicationList()

    stubApiDefinition()

    signInAdminUserGatekeeper
    on(ApplicationsPage)

    When("I select to navigate to the Applications page")
    DashboardPage.selectApplications()

    Then("I am successfully navigated to the Applications page where I can view all applications")
    on(ApplicationsPage)

    When(s"I select to navigate to the application named $applicationName")
    ApplicationsPage.selectByApplicationName(applicationName)

    Then(s"I am successfully navigated to the application named $applicationName")
    on(page)
  }
}
