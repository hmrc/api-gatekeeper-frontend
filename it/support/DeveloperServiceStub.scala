package support

import com.github.tomakehurst.wiremock.client.WireMock._
import model.TopicOptionChoice._
import model.{APICategory, APICategoryDetails, ApiDefinition, User}
import play.api.http.Status
import play.api.libs.json.Json

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

    def primeDeveloperServiceEmailPreferencesByTopicAndCategory(users: Seq[User], topic: TopicOptionChoice, category: APICategoryDetails): Unit = {
    val emailpreferencesByTopicAndCategoryUrl = emailPreferencesUrl+"?topic="+topic.toString+"&regime="+category.category
    stubFor(get(urlEqualTo(emailpreferencesByTopicAndCategoryUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }


    def primeDeveloperServiceEmailPreferencesBySelectedAPisTopicAndCategory(users: Seq[User], selectedApis: Seq[ApiDefinition], topic: TopicOptionChoice): Unit = {
    val categories: Seq[APICategory] = selectedApis.map(_.categories.getOrElse(Seq.empty)).reduce(_ ++ _).distinct

    val topicParam = s"topic=${topic.toString}"
    val regimeParams = categories.map(category => s"&regime=${category.value}").mkString
    val serviceParams = selectedApis.map(api => s"&service=${api.serviceName}").mkString

    val emailpreferencesByTopicAndCategoryUrl = s"$emailPreferencesUrl?$topicParam$regimeParams$serviceParams"
    println("****-"+emailpreferencesByTopicAndCategoryUrl+"-****")
    stubFor(get(urlEqualTo(emailpreferencesByTopicAndCategoryUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }
 

}
