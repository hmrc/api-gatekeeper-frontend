package support

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import model.User
import model.TopicOptionChoice._
import model.APICategory

trait DeveloperServiceStub {
  val emailPreferencesUrl = "/developers/email-preferences"
  val allUrl = "/developers/all"
  val byEmails = "/developers/get-by-emails"

  def primeDeveloperServiceAllSuccessWithUsers(users: Seq[User]): Unit = {

    stubFor(get(urlEqualTo(allUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }

  def primeDeveloperServiceGetByEmails(users: Seq[User]): Unit = {

    stubFor(post(urlEqualTo(byEmails))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }
 
  def primeDeveloperServiceEmailPreferencesByTopic(users: Seq[User], topic: TopicOptionChoice): Unit = {
    val emailpreferencesByTopicUrl = emailPreferencesUrl+"?topic="+topic.toString
    stubFor(get(urlEqualTo(emailpreferencesByTopicUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }

    def primeDeveloperServiceEmailPreferencesByTopicAndCategory(users: Seq[User], topic: TopicOptionChoice, category: APICategory): Unit = {
    val emailpreferencesByTopicAndCategoryUrl = emailPreferencesUrl+"?topic="+topic.toString+"&regime="+category.category
    stubFor(get(urlEqualTo(emailpreferencesByTopicAndCategoryUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }
 

}
