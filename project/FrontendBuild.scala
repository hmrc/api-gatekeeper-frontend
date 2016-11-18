import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object FrontendBuild extends Build with MicroService {

  override val appName = "api-gatekeeper-frontend"

  override lazy val plugins: Seq[Plugins] = Seq(
    SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
  )

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion

  val compile = Seq(
    "uk.gov.hmrc" %% "frontend-bootstrap" % "6.7.0",
    "uk.gov.hmrc" %% "play-authorised-frontend" % "5.8.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "uk.gov.hmrc" %% "play-config" % "2.1.0",
    "uk.gov.hmrc" %% "play-json-logger" % "2.1.1",
    "uk.gov.hmrc" %% "json-encryption" % "2.1.0",
    "uk.gov.hmrc" %% "play-health" % "1.1.0",
    "uk.gov.hmrc" %% "govuk-template" % "4.0.0",
    "uk.gov.hmrc" %% "play-ui" % "4.16.0"
  )

  abstract class TestDependencies(scope: String) {
    lazy val test: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % "2.2.5" % scope,
      "org.scalatestplus" %% "play" % "1.2.0" % scope,
      "org.pegdown" % "pegdown" % "1.4.2" % scope,
      "org.jsoup" % "jsoup" % "1.7.3" % scope,
      "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
      "uk.gov.hmrc" %% "hmrctest" % "1.8.0" % scope,
      "com.github.tomakehurst" % "wiremock" % "1.57" % scope,
      "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % scope,
      "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0",
      "org.scalacheck" %% "scalacheck" % "1.12.5" % scope
    )
  }

  object Test extends TestDependencies("test")

  object AcceptanceTest extends TestDependencies("acceptance")

  def apply() = compile ++ Test.test ++ AcceptanceTest.test
}





