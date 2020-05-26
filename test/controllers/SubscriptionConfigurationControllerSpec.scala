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

package controllers

import org.mockito.BDDMockito.`given`
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{Action, AnyContent, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.WithCSRFAddToken
import play.api.test.Helpers
import services.{ApplicationService, SubscriptionFieldsService}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.Matchers.{eq => eqTo, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionConfigurationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {
  implicit val materializer = fakeApplication.materializer

  trait Setup extends ControllerSetupBase {
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val controller = new SubscriptionConfigurationController(
      mockApplicationService,
      mockAuthConnector
    )(mockConfig, global)
  }

  "list application's subscriptions configuration" in new Setup {
    givenTheUserIsAuthorisedAndIsANormalUser()
    givenTheAppWillBeReturned()
    given(mockConfig.title).willReturn("Unit Test Title")
    given(mockApplicationService.fetchApplicationSubscriptions(eqTo(application.application), eqTo(true))((any[HeaderCarrier])))
      .willReturn(Future.successful(Seq.empty))

    val appId = "123"

    val result : Result = await(controller.listConfigurations(appId)(aLoggedInRequest))

    status(result) shouldBe OK

    titleOf(result) shouldBe "Unit Test Title - Subscription configuration"

    val responseBody = Helpers.contentAsString(result)
    responseBody should include("<h1>Subscription configuration</h1>")

    verify(mockApplicationService)
      .fetchApplication(eqTo(appId))(any[HeaderCarrier])
  }

  // TODO: Copied from AppSpec -> Move to common trait?
  def titleOf(result: Result) = {
    val titleRegEx = """<title[^>]*>(.*)</title>""".r
    val title = titleRegEx.findFirstMatchIn(bodyOf(result)).map(_.group(1))
    title.isDefined shouldBe true
    title.get
  }
}
