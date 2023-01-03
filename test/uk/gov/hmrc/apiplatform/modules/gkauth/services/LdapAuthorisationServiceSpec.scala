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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.FakeRequest
import play.api.mvc.MessagesRequest
import uk.gov.hmrc.internalauth.client.test.FrontendAuthComponentsStub

import uk.gov.hmrc.internalauth.client.test.{FrontendAuthComponentsStub, StubBehaviour}
import uk.gov.hmrc.internalauth.client.Retrieval
import play.api.test.StubControllerComponentsFactory
import play.api.mvc.ControllerComponents
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import scala.concurrent.Future

class LdapAuthorisationServiceSpec extends AsyncHmrcSpec with StubControllerComponentsFactory {
  val fakeRequest = FakeRequest()

  val cc: ControllerComponents = stubMessagesControllerComponents()

  val expectedRetrieval = Retrieval.username ~ Retrieval.hasPredicate(LdapAuthorisationPredicate.gatekeeperReadPermission)

  trait Setup {
    val mockStubBehaviour = mock[StubBehaviour]
    val frontendAuth      = FrontendAuthComponentsStub(mockStubBehaviour)(cc, implicitly)
    val underTest         = new LdapAuthorisationService(frontendAuth)

    protected def stub(
        isAuth: Boolean
      ) = when(mockStubBehaviour.stubAuth(None, expectedRetrieval)).thenReturn(Future.successful(uk.gov.hmrc.internalauth.client.~[Retrieval.Username, Boolean](
      Retrieval.Username("Bob"),
      isAuth
    )))

  }

  trait SessionPresent {
    self: Setup =>
    val msgRequest = new MessagesRequest(fakeRequest.withSession("authToken" -> "Token some-token"), stubMessagesApi())
  }

  trait Authorised {
    self: Setup with SessionPresent =>

    stub(true)
  }

  trait Unauthorised {
    self: Setup with SessionPresent =>

    stub(false)
  }

  trait NoSessionPresent {
    val msgRequest = new MessagesRequest(fakeRequest, stubMessagesApi())
  }

  "return a logged in request when the user has ldap session and is authorised for GK" in new Setup with SessionPresent with Authorised {
    val result = await(underTest.refineLdap(msgRequest))

    result shouldBe 'Right

    inside(result.right.value) { case lir: LoggedInRequest[_] =>
      lir.name shouldBe Some("Bob")
      lir.role shouldBe GatekeeperRoles.READ_ONLY
    }
  }

  "return the original request when the user has ldap session but is NOT authorised for GK" in new Setup with SessionPresent with Unauthorised {
    val result = await(underTest.refineLdap(msgRequest))

    result shouldBe 'Left

    result.left.value shouldBe msgRequest
  }

  "return the original request when the user has no session" in new Setup with NoSessionPresent {
    val result = await(underTest.refineLdap(msgRequest))

    result shouldBe 'Left

    result.left.value shouldBe msgRequest
  }
}
