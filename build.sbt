import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.{baseDirectory, unmanagedSourceDirectories, _}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import bloop.integrations.sbt.BloopDefaults

lazy val appName = "api-gatekeeper-frontend"

Global / bloopAggregateSourceDependencies := true

lazy val AcceptanceTest = config("acceptance") extend Test
lazy val SandboxTest = config("sandbox") extend Test

ThisBuild / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"

ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

scalaVersion := "2.13.8"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=true",
      "dead_code=true"
    ),
    uglify / includeFilter := GlobFilter("apis-*.js"),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(
      concat,
      uglify
    )
  )
  .settings(scalaSettings: _*)
  .settings(
    name:= appName,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    routesGenerator := InjectedRoutesGenerator,
    shellPrompt := (_ => "> "),
    majorVersion := 0,

    Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testCommon",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test"
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(BloopDefaults.configSettings))
  .settings(integrationTestSettings())
  .settings(
    IntegrationTest / parallelExecution := false,
    IntegrationTest / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "testCommon",
    IntegrationTest / unmanagedSourceDirectories += baseDirectory.value / "it"
    )
  .configs(AcceptanceTest)
  .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
  .settings(inConfig(AcceptanceTest)(BloopDefaults.configSettings))
  .settings(
    AcceptanceTest / Keys.fork := false,
    AcceptanceTest / parallelExecution := false,
    AcceptanceTest / testOptions := Seq(Tests.Argument("-l", "SandboxTest", "-eT")),
    AcceptanceTest / testOptions += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("uk.gov.hmrc.gatekeeper.common.AfterHook").newInstance),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "testCommon",
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "acceptance"
    )
  .settings(headerSettings(AcceptanceTest) ++ automateHeaderSettings(AcceptanceTest))

  .configs(SandboxTest)
  .settings(inConfig(SandboxTest)(Defaults.testSettings): _*)
  .settings(inConfig(SandboxTest)(BloopDefaults.configSettings))
  .settings(
    SandboxTest / Keys.fork := false,
    SandboxTest / parallelExecution := false,
    SandboxTest / testOptions := Seq(Tests.Argument("-l", "NonSandboxTest"), Tests.Argument("-n", "SandboxTest", "-eT")),
    SandboxTest / testOptions += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("uk.gov.hmrc.gatekeeper.common.AfterHook").newInstance),
    SandboxTest / unmanagedSourceDirectories += baseDirectory(_ / "acceptance").value
  )
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.gatekeeper.controllers.binders._",
      "uk.gov.hmrc.gatekeeper.models._",
      "uk.gov.hmrc.apiplatform.modules.apis.domain.models._"
    ),
    TwirlKeys.templateImports ++= Seq(
      "views.html.helper.CSPNonce",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.gatekeeper.views.html._",
      "uk.gov.hmrc.gatekeeper.views.html.include._",
      "uk.gov.hmrc.gatekeeper.controllers",
      "uk.gov.hmrc.apiplatform.modules.apis.domain.models._",
      "uk.gov.hmrc.gatekeeper.config.AppConfig"
    )
  )
  .settings(
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )

coverageMinimumStmtTotal := 85.5
coverageFailOnMinimum := true
coverageExcludedPackages := Seq(
  "<empty>",
  """.*\.controllers\.binders""",
  """uk\.gov\.hmrc\.BuildInfo""" ,
  """.*\.Routes""" ,
  """.*\.RoutesPrefix""" ,
  """.*\.Reverse[^.]*""",
  """uk\.gov\.hmrc\.apiplatform\.modules\.common\..*""",
  """uk\.gov\.hmrc\.apiplatform\.modules\.commands\.applications\.domain\.models\..*"""
).mkString(";")