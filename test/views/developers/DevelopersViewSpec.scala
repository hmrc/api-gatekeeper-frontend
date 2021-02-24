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

package views.developers

import model._
import play.api.test.FakeRequest
import views.html.developers.DevelopersView
import views.CommonViewSpec

class DevelopersViewSpec extends CommonViewSpec {

  trait Setup {
    implicit val fakeRequest = FakeRequest

    val developersView = app.injector.instanceOf[DevelopersView]
  }

  val users = Seq(
    RegisteredUser("sample@example.com", UserId.random, "Sample", "Email", false),
    RegisteredUser("another@example.com", UserId.random, "Sample2", "Email", true),
    UnregisteredUser("something@example.com", UserId.random))
  val devs = users.map(u => Developer(u, List.empty))

  "Developers view" should {

    "list all developers" in new Setup {

      val result = developersView.render(devs, "", Map.empty, None, None, None, FakeRequest(), LoggedInUser(None), messagesProvider)
      result.contentType should include( "text/html" )
      users.foreach(user => result.body should include(user.email))
    }
  }
}
