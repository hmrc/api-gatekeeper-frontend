import _root_.play.core.PlayVersion
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

bloopAggregateSourceDependencies in Global := true

lazy val microservice =  (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=false",
      "dead_code=true"
    ),
    includeFilter in uglify := GlobFilter("apis-*.js"),
    pipelineStages := Seq(digest),
    pipelineStages in Assets := Seq(
      concat,
      uglify
    ),
    scalacOptions += "-Ypartial-unification",
  )
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(SilencerSettings())
  .settings(
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.12",
    name:= appName,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator,
    shellPrompt := (_ => "> "),
    majorVersion := 0,

    Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testCommon",
    Test / unmanagedSourceDirectories += baseDirectory.value / "test"
  )
  .settings(inConfig(IntegrationTest)(BloopDefaults.configSettings)) 
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / Keys.fork := false,
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
    AcceptanceTest / testOptions += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("common.AfterHook").newInstance),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "testCommon",
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "acceptance"
  )
  .configs(SandboxTest)
  .settings(inConfig(SandboxTest)(Defaults.testSettings): _*)
  .settings(inConfig(SandboxTest)(BloopDefaults.configSettings))
  .settings(
    SandboxTest / Keys.fork := false,
    SandboxTest / parallelExecution := false,
    SandboxTest / testOptions := Seq(Tests.Argument("-l", "NonSandboxTest"), Tests.Argument("-n", "SandboxTest", "-eT")),
    SandboxTest / testOptions += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("common.AfterHook").newInstance),
    SandboxTest / unmanagedSourceDirectories += baseDirectory(_ / "acceptance").value
  )
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.gatekeeper.controllers.binders._",
      "uk.gov.hmrc.gatekeeper.models._"
    ),
    TwirlKeys.templateImports ++= Seq(
      "views.html.helper.CSPNonce",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.gatekeeper.views.html._",
      "uk.gov.hmrc.gatekeeper.views.html.include._",
      "uk.gov.hmrc.gatekeeper.controllers",
      "uk.gov.hmrc.gatekeeper.config.AppConfig"
    )
  )
  .settings(
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val AcceptanceTest = config("acceptance") extend Test
lazy val SandboxTest = config("sandbox") extend Test
 
lazy val appName = "api-gatekeeper-frontend"

coverageMinimumStmtTotal := 84
coverageFailOnMinimum := true
coverageExcludedPackages := Seq(
  "<empty>",
  "com.kenshoo.play.metrics",
  ".*definition.*",
  "prod",
  "testOnlyDoNotUseInAppConf",
  "app",
  "uk.gov.hmrc.BuildInfo"
).mkString(";")
  