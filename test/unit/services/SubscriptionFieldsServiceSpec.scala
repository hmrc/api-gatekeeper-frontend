/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.services

import java.util.UUID

import connectors._
import model.ApiSubscriptionFields.{Fields, SubscriptionField, SubscriptionFields}
import model.{ApplicationResponse, ApplicationState, FieldsDeleteSuccessResult, Standard}
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{spy, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import services.SubscriptionFieldsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionFieldsServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  val apiContext: String = "sub-ser-test"
  val apiVersion: String = "1.0"
  val applicationName: String = "third-party-application"
  val clientId = "clientId"
  val mockSandboxSubscriptionFieldsConnector = mock[SandboxSubscriptionFieldsConnector]
  val mockProductionSubscriptionFieldsConnector = mock[ProductionSubscriptionFieldsConnector]
  val application = ApplicationResponse(
    UUID.randomUUID(), clientId, "gatewayId", applicationName, "PRODUCTION", None, Set.empty, DateTime.now(), Standard(), ApplicationState())

  trait Setup {
    lazy val locked = false
    val response = mock[WSResponse]

    implicit val hc = HeaderCarrier()

    val service = new SubscriptionFieldsService(mockSandboxSubscriptionFieldsConnector, mockProductionSubscriptionFieldsConnector)
    val underTest = spy(service)
  }

  "fetchFields" should {

    "return custom fields for a given application in the correct environment (fields populated)" in new Setup {
      val fieldsId = UUID.randomUUID()
      val fieldValuesResponse: SubscriptionFields =
        SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, Fields("field1" -> "val001", "field2" -> "val002"))
      val fieldDefinitions = List(SubscriptionField("field1", "desc1", "hint1", "some type"), SubscriptionField("field2", "desc2", "hint2", "some other type"))
      val mergedDefValues = List(
        SubscriptionField("field1", "desc1", "hint1", "some type", Some("val001")),
        SubscriptionField("field2", "desc2", "hint2", "some other type", Some("val002"))
      )

      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))
        .willReturn(Future.successful(Some(fieldValuesResponse)))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion))
        .willReturn(Future.successful(fieldDefinitions))

      val result: Seq[SubscriptionField] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe mergedDefValues

      verify(underTest).connectorFor(application)
    }

    "return custom fields for a given application in the correct environment (fields not populated)" in new Setup {
      val fieldDefinitions = List(SubscriptionField("field1", "desc1", "hint1", "some type"), SubscriptionField("field2", "desc2", "hint2", "some other type"))

      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(fieldDefinitions))

      val result: Seq[SubscriptionField] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe fieldDefinitions

      verify(underTest).connectorFor(application)
    }

    "return empty sequence if no definitions have been found in the correct environment" in new Setup {
      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(Seq.empty))

      val result: Seq[SubscriptionField] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe Seq.empty[SubscriptionField]

      verify(underTest).connectorFor(application)
    }
  }

  "saveFieldsValues" should {
    "save the field values in the correct environment" in new Setup {
      val fieldsId = UUID.randomUUID()
      val fields = Fields("field1" -> "val001", "field2" -> "val002")
      val fieldValuesResponse: SubscriptionFields = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, fields)

      given(mockProductionSubscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fields))
        .willReturn(Future.successful(HttpResponse(CREATED)))

      val result = await(underTest.saveFieldValues(application, apiContext, apiVersion, fields))

      verify(mockProductionSubscriptionFieldsConnector).saveFieldValues(clientId, apiContext, apiVersion, fields)

      verify(underTest).connectorFor(application)
    }
  }

  "deleteFieldValues" should {
    "delete the field values in the correct environment" in new Setup {
      val fieldsId = UUID.randomUUID()
      val fields = Fields("field1" -> "val001", "field2" -> "val002")
      val fieldValuesResponse: SubscriptionFields = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, fields)

      given(mockProductionSubscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))
        .willReturn(Future.successful(FieldsDeleteSuccessResult))

      val result = await(underTest.deleteFieldValues(application, apiContext, apiVersion))

      verify(mockProductionSubscriptionFieldsConnector).deleteFieldValues(clientId, apiContext, apiVersion)

      verify(underTest).connectorFor(application)
    }
  }

  "connectorFor" should {
    "return the production api scope connector for an application deployed to production" in new Setup {
      val app = application.copy(deployedTo = "PRODUCTION")

      val result = underTest.connectorFor(app)

      result shouldBe mockProductionSubscriptionFieldsConnector
    }

    "return the sandbox api scope connector for an application deployed to sandbox" in new Setup {
      val app = application.copy(deployedTo = "SANDBOX")

      val result = underTest.connectorFor(app)

      result shouldBe mockSandboxSubscriptionFieldsConnector
    }
  }
}
