import _root_.play.core.PlayVersion
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import play.routes.compiler.InjectedRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys.{baseDirectory, unmanagedSourceDirectories, _}
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

import scala.util.Properties

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
      )
    )
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.11",
      name:= appName,
      libraryDependencies ++= appDependencies,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := InjectedRoutesGenerator,
      shellPrompt := (_ => "> "),
      majorVersion := 0
    )
    .configs(IntegrationTest)
    .settings(
      Keys.fork in IntegrationTest := false,
      Defaults.itSettings,
      parallelExecution in IntegrationTest := false,
      unmanagedSourceDirectories in IntegrationTest += baseDirectory(_ / "it").value
    )
    .configs(AcceptanceTest)
    .settings(
      inConfig(AcceptanceTest)(Defaults.testSettings): _*
    )
    .settings(
      Keys.fork in AcceptanceTest := false,
      parallelExecution in AcceptanceTest := false,
      testOptions in AcceptanceTest := Seq(Tests.Argument("-l", "SandboxTest", "-eT")),
      testOptions in AcceptanceTest += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("acceptance.AfterHook").newInstance),
      unmanagedSourceDirectories in AcceptanceTest += baseDirectory(_ / "acceptance").value
    )
    .configs(SandboxTest)
    .settings(
      inConfig(SandboxTest)(Defaults.testSettings): _*
    )
    .settings(
      Keys.fork in SandboxTest := false,
      parallelExecution in SandboxTest := false,
      testOptions in SandboxTest := Seq(Tests.Argument("-l", "NonSandboxTest"), Tests.Argument("-n", "SandboxTest", "-eT")),
      testOptions in SandboxTest += Tests.Cleanup((loader: java.lang.ClassLoader) => loader.loadClass("acceptance.AfterHook").newInstance),
      unmanagedSourceDirectories in SandboxTest += baseDirectory(_ / "acceptance").value
    )
    .settings(
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases"),
        Resolver.jcenterRepo)
    )
    .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest
lazy val AcceptanceTest = config("acceptance") extend Test
lazy val SandboxTest = config("sandbox") extend Test
lazy val appName = "api-gatekeeper-frontend"

lazy val slf4jVersion = "1.7.23"
lazy val logbackVersion = "1.1.10"
lazy val hmrctestVersion = "3.9.0-play-25"
lazy val jsoupVersion = "1.12.1"
lazy val scalaCheckVersion = "1.14.0"

lazy val acceptanceTestDeps: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "acceptance",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "acceptance",
  "org.pegdown" % "pegdown" % "1.6.0" % "acceptance",
  "org.jsoup" % "jsoup" % jsoupVersion % "acceptance",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "acceptance",
  "uk.gov.hmrc" %% "hmrctest" % hmrctestVersion % "acceptance",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "acceptance",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % "acceptance",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % "acceptance",
  "org.mockito" % "mockito-all" % "1.10.19" % "acceptance",
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "acceptance"
).map(
  _.excludeAll(
    ExclusionRule(organization = "commons-logging"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.apache.logging.log4j"),
    ExclusionRule("org.apache.logging.log4j", "log4j-core"),
    ExclusionRule("org.slf4j", "slf4j-log412"),
    ExclusionRule("org.slf4j", "slf4j-jdk14")
  ))

lazy val testScope = "test,it"

lazy val testDeps: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % testScope,
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % testScope,
  "org.pegdown" % "pegdown" % "1.6.0" % testScope,
  "org.jsoup" % "jsoup" % jsoupVersion % testScope,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % testScope,
  "uk.gov.hmrc" %% "hmrctest" % hmrctestVersion % testScope,
  "com.github.tomakehurst" % "wiremock" % "1.58" % testScope,
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % testScope,
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % testScope,
  "org.mockito" % "mockito-all" % "1.10.19" % testScope,
  "org.scalacheck" %% "scalacheck" % scalaCheckVersion % testScope
).map(
  _.excludeAll(
    ExclusionRule(organization = "commons-logging"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.apache.logging.log4j"),
    ExclusionRule("org.apache.logging.log4j", "log4j-core"),
    ExclusionRule("org.slf4j", "slf4j-log412"),
    ExclusionRule("org.slf4j", "slf4j-jdk14")
  ))
lazy val appDependencies: Seq[ModuleID] = compile ++ testDeps ++ acceptanceTestDeps
lazy val allPhases = "tt->test;test->test;test->compile;compile->compile"

lazy val allItPhases = "tit->it;it->it;it->compile;compile->compile"
lazy val compile = Seq(
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.13.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.36.0-play-25",
  "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-25",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "1.1.0-play-25",
  "uk.gov.hmrc" %% "json-encryption" % "4.4.0-play-25",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.5.0",
  "uk.gov.hmrc" %% "emailaddress" % "3.2.0",
  "commons-net" % "commons-net" % "3.6",
  "org.slf4j" % "slf4j-api" % slf4jVersion,
  "org.slf4j" % "jcl-over-slf4j" % slf4jVersion,
  "org.slf4j" % "log4j-over-slf4j" % slf4jVersion,
  "org.slf4j" % "jul-to-slf4j" % slf4jVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "ch.qos.logback" % "logback-core" % logbackVersion
).map(
  _.excludeAll(
    ExclusionRule(organization = "commons-logging"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.apache.logging.log4j"),
    ExclusionRule("org.slf4j", "slf4j-log412"),
    ExclusionRule("org.slf4j", "slf4j-jdk14")
  ))


def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq(s"-Dtest.name=${test.name}", s"-Dtest_driver=${Properties.propOrElse("test_driver", "chrome")}"))))
  }

coverageMinimum := 82
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
