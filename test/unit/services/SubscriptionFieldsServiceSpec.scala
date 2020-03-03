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

package unit.services

import java.util.UUID

import connectors._
import model.apiSubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFields, fields}
import model.{ApplicationResponse, ApplicationState, FieldsDeleteSuccessResult, Standard}
import org.joda.time.DateTime
import org.mockito.BDDMockito.given
import org.mockito.Mockito.{never, spy, verify}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.Status._
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
  private val mockSandboxSubscriptionFieldsConnector = mock[SandboxSubscriptionFieldsConnector]
  private val mockProductionSubscriptionFieldsConnector = mock[ProductionSubscriptionFieldsConnector]
  private val application = ApplicationResponse(
    UUID.randomUUID(), clientId, "gatewayId", applicationName, "PRODUCTION", None, Set.empty, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

  trait Setup {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service = new SubscriptionFieldsService(mockSandboxSubscriptionFieldsConnector, mockProductionSubscriptionFieldsConnector)
    val underTest: SubscriptionFieldsService = spy(service)
  }

  "fetchFields" should {

    "return custom fields for a given application in the correct environment (fields populated)" in new Setup {
      private val fieldsId = UUID.randomUUID()
      val fieldValuesResponse: SubscriptionFields =
        SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, fields("field1" -> "val001", "field2" -> "val002"))
      val fieldDefinitions = List(
        SubscriptionFieldDefinition("field1", "desc1", "hint1", "some type"),
        SubscriptionFieldDefinition("field2", "desc2", "hint2", "some other type")
      )

      val mergedDefValues = List(
        SubscriptionFieldValue("field1", "desc1", "hint1", "some type", Some("val001")),
        SubscriptionFieldValue("field2", "desc2", "hint2", "some other type", Some("val002"))
      )

      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion))
        .willReturn(Future.successful(Some(fieldValuesResponse)))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion))
        .willReturn(Future.successful(fieldDefinitions))

      val result: Seq[SubscriptionFieldValue] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe mergedDefValues

      verify(underTest).connectorFor(application)
    }

    "return custom fields for a given application in the correct environment (fields not populated)" in new Setup {
      val fieldDefinitions = List(
        SubscriptionFieldDefinition("field1", "desc1", "hint1", "some type"),
        SubscriptionFieldDefinition("field2", "desc2", "hint2", "some other type")
      )

      val fieldValues = List(
        SubscriptionFieldValue("field1", "desc1", "hint1", "some type", None),
        SubscriptionFieldValue("field2", "desc2", "hint2", "some other type", None)
      )

      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(fieldDefinitions))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))

      val result: Seq[SubscriptionFieldValue] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe fieldValues

      verify(underTest).connectorFor(application)
    }

    "return custom fields for a given application in the correct environment when there are no fields defined, does not try and get the values" in new Setup {
      private val fieldDefinitions = List.empty

      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(fieldDefinitions))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))

      val result: Seq[SubscriptionFieldValue] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe fieldDefinitions

      verify(underTest).connectorFor(application)

      verify(mockProductionSubscriptionFieldsConnector,never()).fetchFieldValues(clientId, apiContext, apiVersion)
    }


    "return empty sequence if no definitions have been found in the correct environment" in new Setup {
      given(mockProductionSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))
      given(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(Seq.empty))

      val result: Seq[SubscriptionFieldValue] = await(underTest.fetchFields(application, apiContext, apiVersion))
      result shouldBe Seq.empty[SubscriptionFieldValue]

      verify(underTest).connectorFor(application)
    }
  }

  "saveFieldsValues" should {
    "save the field values in the correct environment" in new Setup {
      private val fieldsId = UUID.randomUUID()
      private val fieldsToSave = fields("field1" -> "val001", "field2" -> "val002")
      val fieldValuesResponse: SubscriptionFields = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, fieldsToSave)

      given(mockProductionSubscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fieldsToSave))
        .willReturn(Future.successful(HttpResponse(CREATED)))

      await(underTest.saveFieldValues(application, apiContext, apiVersion, fieldsToSave))

      verify(mockProductionSubscriptionFieldsConnector).saveFieldValues(clientId, apiContext, apiVersion, fieldsToSave)

      verify(underTest).connectorFor(application)
    }
  }

  "deleteFieldValues" should {
    "delete the field values in the correct environment" in new Setup {

      given(mockProductionSubscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion))
        .willReturn(Future.successful(FieldsDeleteSuccessResult))

      await(underTest.deleteFieldValues(application, apiContext, apiVersion))

      verify(mockProductionSubscriptionFieldsConnector).deleteFieldValues(clientId, apiContext, apiVersion)

      verify(underTest).connectorFor(application)
    }
  }

  "connectorFor" should {
    "return the production api scope connector for an application deployed to production" in new Setup {
      private val app = application.copy(deployedTo = "PRODUCTION")

      private val result = underTest.connectorFor(app)

      result shouldBe mockProductionSubscriptionFieldsConnector
    }

    "return the sandbox api scope connector for an application deployed to sandbox" in new Setup {
      private val app = application.copy(deployedTo = "SANDBOX")

      private val result = underTest.connectorFor(app)

      result shouldBe mockSandboxSubscriptionFieldsConnector
    }
  }
}
