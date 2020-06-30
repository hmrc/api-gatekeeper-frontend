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

package views.developers

import model.{LoggedInUser, _}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.MessagesProvider
import play.api.test.FakeRequest
import views.html.developers.Developers

class DevelopersViewSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  trait Setup {
    implicit val fakeRequest = FakeRequest
    val developersView = app.injector.instanceOf[Developers]
    val messagesProvider = app.injector.instanceOf[MessagesProvider]
  }
  val users = Seq(
    User("sample@example.com", "Sample", "Email", Some(false)),
    User("another@example.com", "Sample2", "Email", Some(true)),
    UnregisteredCollaborator("something@example.com"))
  val devs = users.map(u => Developer.createFromUser(u, Seq.empty))

  "Developers view" must {

    "list all developers" in new Setup {

      val result = developersView.render(devs, "", Map.empty, None, None, None, FakeRequest(), LoggedInUser(None), messagesProvider)
      result.contentType must include( "text/html" )
      users.foreach(user => result.body must include(user.email))
    }
  }
}
