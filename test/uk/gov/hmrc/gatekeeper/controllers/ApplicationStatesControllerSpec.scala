/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.controllers

import mocks.services.ApplicationServiceMockProvider
import play.api.http.Status.{FORBIDDEN, OK}
import play.api.test.Helpers.{contentAsString, contentType, header, running, status}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.gatekeeper.models.ApplicationStateHistoryChange

import scala.concurrent.ExecutionContext.Implicits.global

class ApplicationStatesControllerSpec extends ControllerBaseSpec with ApplicationServiceMockProvider
    with StrideAuthorisationServiceMockModule with LdapAuthorisationServiceMockModule {
  implicit val materializer = app.materializer

  running(app) {
    trait Setup extends ControllerSetupBase {
      val underTest = new ApplicationStatesController(mcc, mockApplicationService, StrideAuthorisationServiceMock.aMock, LdapAuthorisationServiceMock.aMock)
    }

    "csv" should {
      val appStateHistoryChanges = Array(
        ApplicationStateHistoryChange(ApplicationId.random.value.toString, "app 1 name", "app 1 version", "old state 1", "old ts 1", "new state 1", "new ts 1"),
        ApplicationStateHistoryChange(ApplicationId.random.value.toString, "app 2 name", "app 2 version", "old state 2", "old ts 2", "new state 2", "new ts 2")
      )
      val expectedCsv            = "applicationId,applicationName,journeyVersion,oldState,oldTimestamp,newState,newTimestamp\n" +
        appStateHistoryChanges.map(c => s"${c.applicationId},${c.appName},${c.journeyVersion},${c.oldState},${c.oldTimestamp},${c.newState},${c.newTimestamp}").mkString(
          "",
          "\n",
          "\n"
        )

      "return csv data for ldap authorised user" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        LdapAuthorisationServiceMock.Auth.succeeds
        ApplicationServiceMock.FetchProdAppStateHistories.thenReturn(appStateHistoryChanges: _*)
        val result = underTest.csv()(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedCsv
      }

      "return csv data for stride authorised user" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        ApplicationServiceMock.FetchProdAppStateHistories.thenReturn(appStateHistoryChanges: _*)
        val result = underTest.csv()(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedCsv
      }

      "return csv data with correct headers" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        LdapAuthorisationServiceMock.Auth.succeeds
        ApplicationServiceMock.FetchProdAppStateHistories.thenReturn(appStateHistoryChanges: _*)
        val result = underTest.csv()(aLoggedInRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/csv")
        header("Content-Disposition", result) shouldBe Some("attachment;filename=applicationStateHistory.csv")
      }

      "fails for unauthorised users" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        LdapAuthorisationServiceMock.Auth.notAuthorised
        val result = underTest.csv()(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
      }
    }
  }
}
