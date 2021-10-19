/*
 * Copyright 2021 HM Revenue & Customs
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

package mocks.connectors

import org.mockito.ArgumentMatchersSugar
import org.mockito.MockitoSugar
import connectors.ApiCataloguePublishConnector
import connectors.ApiCataloguePublishConnector._
import uk.gov.hmrc.http.Upstream5xxResponse
import scala.concurrent.Future.successful


trait ApiCataloguePublishConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>
  val mockApiCataloguePublishConnector = mock[ApiCataloguePublishConnector]

  object ApiCataloguePublishConnectorMock {

    object PublishAll {
      def returnRight() = when(mockApiCataloguePublishConnector.publishAll()(*)).thenReturn(successful(Right(PublishAllResponse(message = "Publish all called and is working in the background, check application logs for progress"))))
      
      def returnLeft() = when(mockApiCataloguePublishConnector.publishAll()(*)).thenReturn(successful(Left(Upstream5xxResponse("error", 500, 500, Map.empty))))
    }

    object PublishByServiceName {
      def returnRight() = when(mockApiCataloguePublishConnector.publishByServiceName(*)(*)).thenReturn(successful(Right(PublishResponse(id ="id", publisherReference = "publisherReference", platformType = "platformType"))))
      
      def returnLeft() = when(mockApiCataloguePublishConnector.publishByServiceName(*)(*)).thenReturn(successful(Left(Upstream5xxResponse("error", 500, 500, Map.empty))))
    }
  }

}