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
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import bloop.integrations.sbt.BloopDefaults

lazy val slf4jVersion = "1.7.23"
lazy val logbackVersion = "1.1.10"
lazy val jsoupVersion = "1.12.1"
lazy val scalaCheckVersion = "1.14.0"

lazy val dependencies = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.16.0",
  "uk.gov.hmrc" %% "time" % "3.11.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.55.0-play-26",
  "uk.gov.hmrc" %% "play-ui" % "8.11.0-play-26",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.2.0-play-26",
  "uk.gov.hmrc" %% "json-encryption" % "4.8.0-play-26",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.11.0",
  "uk.gov.hmrc" %% "emailaddress" % "3.4.0",
  "commons-net" % "commons-net" % "3.6",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
  "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
  "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion,
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "com.typesafe.play" %% "play-json-joda" % "2.8.1",
  "org.typelevel" %% "cats-core" % "2.3.1"
)

lazy val testDependencies: Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % testScope,
  "org.pegdown" % "pegdown" % "1.6.0" % testScope,
  "org.jsoup" % "jsoup" % jsoupVersion % testScope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope,
  "com.github.tomakehurst" % "wiremock" % "1.58" % testScope,
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % testScope,
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % testScope,
  "org.mockito" %% "mockito-scala-scalatest" % "1.7.1" % testScope,
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % testScope
)

lazy val acceptanceTestDependencies: Seq[ModuleID] = Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % "acceptance",
  "org.pegdown" % "pegdown" % "1.6.0" % "acceptance",
  "org.jsoup" % "jsoup" % jsoupVersion % "acceptance",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "acceptance",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "acceptance",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % "acceptance",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % "acceptance",
  "org.mockito" %% "mockito-scala-scalatest" % "1.7.1" % "acceptance",
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "acceptance"
)

lazy val appDependencies: Seq[ModuleID] = dependencies ++ testDependencies ++ acceptanceTestDependencies

lazy val microservice =  (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
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
    routesImport += "controllers.binders._"
  )
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(SilencerSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    scalaVersion := "2.12.11",
    name:= appName,
    libraryDependencies ++= appDependencies,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesGenerator := InjectedRoutesGenerator,
    shellPrompt := (_ => "> "),
    majorVersion := 0,
    routesImport += "controllers.binders._",
    Test / unmanagedSourceDirectories += baseDirectory(_ / "testCommon").value,
    Test / unmanagedSourceDirectories += baseDirectory(_ / "test").value  
  )
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    IntegrationTest / Keys.fork := false,
    IntegrationTest / parallelExecution := false,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "testCommon").value,
    IntegrationTest / unmanagedSourceDirectories += baseDirectory(_ / "it").value  
  )
  .configs(AcceptanceTest)
  .settings(
    inConfig(AcceptanceTest)(Defaults.testSettings): _*
  )
  .settings(inConfig(AcceptanceTest)(BloopDefaults.configSettings))
  .settings(
    AcceptanceTest / Keys.fork := false,
    AcceptanceTest / parallelExecution := false,
    AcceptanceTest / testOptions := Seq(Tests.Argument("-l", "SandboxTest", "-eT")),
    AcceptanceTest / testOptions += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("acceptance.AfterHook").newInstance),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory(_ / "testCommon").value,
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory(_ / "acceptance").value
  )
  .configs(SandboxTest)
  .settings(
    inConfig(SandboxTest)(Defaults.testSettings): _*
  )
  .settings(
    SandboxTest / Keys.fork := false,
    SandboxTest / parallelExecution := false,
    SandboxTest / testOptions := Seq(Tests.Argument("-l", "NonSandboxTest"), Tests.Argument("-n", "SandboxTest", "-eT")),
    SandboxTest / testOptions += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("acceptance.AfterHook").newInstance),
    SandboxTest / unmanagedSourceDirectories += baseDirectory(_ / "acceptance").value
  )
  .settings(
    resolvers := Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.typesafeRepo("releases"),
      Resolver.jcenterRepo
    )
  )
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)


lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest
lazy val AcceptanceTest = config("acceptance") extend Test
lazy val SandboxTest = config("sandbox") extend Test

 
lazy val appName = "api-gatekeeper-frontend"

lazy val testScope = "test,it"
lazy val allPhases = "tt->test;test->test;test->compile;compile->compile"
lazy val allItPhases = "tit->it;it->it;it->compile;compile->compile"

coverageMinimum := 85
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
