/*
 * Copyright 2016 HM Revenue & Customs
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

package acceptance.pages

import acceptance.WebPage

trait DynamicPage extends WebPage {
  val pageHeading: String

  override def isCurrentPage: Boolean = find(tagName("h1")).fold(false)({
    e => e.text == pageHeading
  })
}

case class ReviewPage(applicationId: String, applicationName: String) extends DynamicPage {
  override val pageHeading = applicationName
  override val url: String = s"http://localhost:9000/api-gatekeeper/review?id=$applicationId"
}

case class ApprovedPage(applicationId: String, applicationName: String) extends DynamicPage {
  override val pageHeading = applicationName
  override val url: String = s"http://localhost:9000/api-gatekeeper/approved?id=$applicationId"
}

case class ResendVerificationPage(applicationId: String, applicationName: String) extends DynamicPage {
  override val pageHeading = applicationName
  override val url: String = s"http://localhost:9000/api-gatekeeper/applications/$applicationId/resend-verification"
}