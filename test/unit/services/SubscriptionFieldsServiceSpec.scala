/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.service

import java.util.UUID

import connectors.SubscriptionFieldsConnector
import model.ApiSubscriptionFields.{Fields, SubscriptionField, SubscriptionFields}
import model.FieldsDeleteSuccessResult
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.ws.WSResponse
import services.SubscriptionFieldsService
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class SubscriptionFieldsServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  val apiContext: String = "sub-ser-test"
  val apiVersion: String = "1.0"
  val applicationName: String = "third-party-application"
  val clientId = "clientId"
  val mockSubscriptionFieldsConnector = mock[SubscriptionFieldsConnector]

  trait Setup {
    lazy val locked = false
    val response = mock[WSResponse]

    implicit val hc = HeaderCarrier()

    val underTest = new SubscriptionFieldsService {
      override val subscriptionFieldsConnnector = mockSubscriptionFieldsConnector
    }
  }

  "fetchFields" should {

    "return custom fields for a given application (fields populated)" in new Setup {
      val fieldsId = UUID.randomUUID()
      val fieldValuesResponse: SubscriptionFields = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, Fields("field1" -> "val001", "field2" -> "val002"))
      val fieldDefinitions = List(SubscriptionField("field1", "desc1", "hint1", "some type"), SubscriptionField("field2", "desc2", "hint2", "some other type"))
      val mergedDefValues = List(SubscriptionField("field1", "desc1", "hint1", "some type", Some("val001")), SubscriptionField("field2", "desc2", "hint2", "some other type", Some("val002")))

      given(mockSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(Some(fieldValuesResponse)))
      given(mockSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(fieldDefinitions))

      val result: Seq[SubscriptionField] = await(underTest.fetchFields(clientId, apiContext, apiVersion))
      result shouldBe mergedDefValues
    }

    "return custom fields for a given application (fields not populated)" in new Setup {
      val fieldDefinitions = List(SubscriptionField("field1", "desc1", "hint1", "some type"), SubscriptionField("field2", "desc2", "hint2", "some other type"))

      given(mockSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))
      given(mockSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(fieldDefinitions))

      val result: Seq[SubscriptionField] = await(underTest.fetchFields(clientId, apiContext, apiVersion))
      result shouldBe fieldDefinitions
    }

    "return empty sequence if no definitions have been found" in new Setup {
      given(mockSubscriptionFieldsConnector.fetchFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(None))
      given(mockSubscriptionFieldsConnector.fetchFieldDefinitions(apiContext, apiVersion)).willReturn(Future.successful(Seq.empty))

      val result: Seq[SubscriptionField] = await(underTest.fetchFields(clientId, apiContext, apiVersion))
      result shouldBe Seq.empty[SubscriptionField]
    }
  }

  "saveFieldsValues" should {
    "save the field values" in new Setup {
      val fieldsId = UUID.randomUUID()
      val fields = Fields("field1" -> "val001", "field2" -> "val002")
      val fieldValuesResponse: SubscriptionFields = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, fields)

      given(mockSubscriptionFieldsConnector.saveFieldValues(clientId, apiContext, apiVersion, fields)).willReturn(Future.successful(HttpResponse(201)))

      val result = await(underTest.saveFieldValues(clientId, apiContext, apiVersion, fields))

      verify(mockSubscriptionFieldsConnector).saveFieldValues(clientId, apiContext, apiVersion, fields)
    }
  }

  "deleteFieldValues" should {
    "delete the field values" in new Setup {
      val fieldsId = UUID.randomUUID()
      val fields = Fields("field1" -> "val001", "field2" -> "val002")
      val fieldValuesResponse: SubscriptionFields = SubscriptionFields(clientId, apiContext, apiVersion, fieldsId, fields)

      given(mockSubscriptionFieldsConnector.deleteFieldValues(clientId, apiContext, apiVersion)).willReturn(Future.successful(FieldsDeleteSuccessResult))

      val result = await(underTest.deleteFieldValues(clientId, apiContext, apiVersion))

      verify(mockSubscriptionFieldsConnector).deleteFieldValues(clientId, apiContext, apiVersion)
    }
  }
}
