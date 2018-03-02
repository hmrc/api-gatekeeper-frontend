package acceptance.specs

import acceptance.pages.{ApplicationPage, ApplicationsPage, DashboardPage, DeleteApplicationPage}
import acceptance.{BaseSpec, SignInSugar}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import component.matchers.CustomMatchers
import org.openqa.selenium.By
import org.scalatest.{GivenWhenThen, Matchers, Tag}

import scala.io.Source

class APIGatekeeperDeleteApplicationSpec extends BaseSpec with SignInSugar with Matchers with CustomMatchers with MockDataSugar with GivenWhenThen {

  feature("Delete an application") {
    scenario("View the delete page for an application") {
      Given("I have successfully logged in to the API Gatekeeper")
      stubApplicationList

      val applicationsList = Source.fromURL(getClass.getResource("/applications.json")).mkString.replaceAll("\n","")

      stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationsList).withStatus(200)))

      stubApplicationSubscription
      stubApiDefinition

      signInSuperUserGatekeeper
      on(DashboardPage)

      When("I select to navigate to the Applications page")
      DashboardPage.selectApplications()

      Then("I am successfully navigated to the Applications page where I can view all applications")
      on(ApplicationsPage)

      stubApplication

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("Automated Test Application")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      When("I select the Delete Application Button")
      ApplicationPage.selectDeleteApplication

      Then("I am successfully navigated to the Delete Application page")
      on(DeleteApplicationPage)
    }
  }

  def stubApplicationList = {
    stubFor(get(urlEqualTo("/gatekeeper/applications")).willReturn(aResponse().withBody(approvedApplications).withStatus(200)))
    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applications).withStatus(200)))
  }

  def stubApplication = {
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(application).withStatus(200)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216")).willReturn(aResponse().withBody(application).withStatus(200)))
    stubFor(get(urlEqualTo("/gatekeeper/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff")).willReturn(aResponse().withBody(application).withStatus(200)))
    stubFor(get(urlEqualTo("/gatekeeper/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(200)))
    stubFor(get(urlEqualTo("/application/fa38d130-7c8e-47d8-abc0-0374c7f73216/subscription")).willReturn(aResponse().withBody("[]").withStatus(200)))
  }

  def stubApplicationListWithNoSubs = {
    stubFor(get(urlEqualTo("/gatekeeper/applications")).willReturn(aResponse().withBody(approvedApplications).withStatus(200)))
    stubFor(get(urlEqualTo("/application")).willReturn(aResponse().withBody(applicationWithNoSubscription).withStatus(200)))
  }

  def stubApiDefinition = {
    stubFor(get(urlEqualTo("/api-definition")).willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
    stubFor(get(urlEqualTo("/api-definition?type=private")).willReturn(aResponse().withStatus(200).withBody(apiDefinition)))
  }

  def stubApplicationSubscription = {
    stubFor(get(urlEqualTo("/application/subscriptions")).willReturn(aResponse().withBody(applicationSubscription).withStatus(200)))
  }

  def stubApplicationDelete = {
    stubFor(get(urlEqualTo("/application/df0c32b6-bbb7-46eb-ba50-e6e5459162ff/delete")))
  }

  private def assertNumberOfApplicationsPerPage(expected: Int) = {
    webDriver.findElements(By.cssSelector("tbody > tr")).size() shouldBe expected
  }

  private def assertApplicationsList(devList: Seq[((String, String, String, String), Int)]) {
    for ((app, index) <- devList) {
      val fn = webDriver.findElement(By.id(s"app-name-$index")).getText shouldBe app._1
      val sn = webDriver.findElement(By.id(s"app-created-$index")).getText shouldBe app._2
      val em = webDriver.findElement(By.id(s"app-subs-$index")).getText shouldBe app._3
      val st = webDriver.findElement(By.id(s"app-status-$index")).getText shouldBe app._4
    }
  }
}
