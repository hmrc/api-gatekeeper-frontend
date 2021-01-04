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

package mocks.config

import config.AppConfig
import mocks.TestRoles
import org.mockito.BDDMockito.`given`
import org.scalatestplus.mockito.MockitoSugar

trait AppConfigMock extends MockitoSugar with TestRoles {
  implicit val mockConfig = mock[AppConfig]

  given(mockConfig.title).willReturn("Unit Test Title")

  given(mockConfig.userRole).willReturn(userRole)
  given(mockConfig.adminRole).willReturn(adminRole)
  given(mockConfig.superUserRole).willReturn(superUserRole)
  given(mockConfig.superUsers).willReturn(Seq("superUserName"))

  given(mockConfig.gatekeeperSuccessUrl).willReturn("http://mock-gatekeeper-frontend/api-gatekeeper/applications")
  given(mockConfig.strideLoginUrl).willReturn("https://loginUri")
  given(mockConfig.appName).willReturn("Gatekeeper app name")

}
