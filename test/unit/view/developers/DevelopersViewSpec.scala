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

package unit.view.developers

import config.AppConfig
import model._
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class DevelopersViewSpec extends PlaySpec with OneServerPerSuite {

  val users = Seq(
    User("sample@email.com", "Sample", "Email", Some(false)),
    User("another@email.com", "Sample2", "Email", Some(true)),
    UnregisteredCollaborator("something@email.com"))
  val devs = users.map(u => Developer.createFromUser(u, Seq.empty))

  "Developers view" must {

    "list all developers" in {
      implicit val fakeRequest = FakeRequest
      val result = views.html.developers.developers.render(devs, "", Map.empty, None, None, FakeRequest(), None, applicationMessages, AppConfig)
      result.contentType must include( "text/html" )
      users.foreach(user => result.body must include(user.email))
    }
  }
}
