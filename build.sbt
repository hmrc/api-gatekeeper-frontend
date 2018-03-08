import _root_.play.core.PlayVersion
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import play.routes.compiler.StaticRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

lazy val slf4jVersion = "1.7.23"
lazy val logbackVersion = "1.1.10"

lazy val microservice =  (project in file("."))
    .enablePlugins(Seq(_root_.play.sbt.PlayScala) ++ plugins: _*)
    .settings(
      Concat.groups := Seq(
        "javascripts/apis-app.js" -> group(
          (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
        )
      ),
      UglifyKeys.compressOptions := Seq(
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
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.11",
      name:= appName,
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := StaticRoutesGenerator,
      shellPrompt := (_ => "> ")
    )
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in Test := Seq(Tests.Filter(unitFilter)),
      addTestReportOption(Test, "test-reports"),
      unmanagedSourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest) (base => Seq(base / "test/unit")),
      unmanagedResourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest) (base => Seq(base / "test/unit"))
    )
    .configs(AcceptanceTest)
    .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
    .settings(
      testOptions in AcceptanceTest := Seq(Tests.Filter(acceptanceFilter), Tests.Argument("-l", "SandboxTest")),
      unmanagedSourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest) (base => Seq(base / "test")),
      unmanagedResourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest) (base => Seq(base / "test")),
      unmanagedResourceDirectories in AcceptanceTest <+= baseDirectory(_ / "target/web/public/test"),
      Keys.fork in AcceptanceTest := false,
      parallelExecution in AcceptanceTest := false,
      addTestReportOption(AcceptanceTest, "acceptance-test-reports")
    )
    .configs(SandboxTest)
    .settings(inConfig(SandboxTest)(Defaults.testTasks): _*)
    .settings(
      testOptions in SandboxTest := Seq(Tests.Argument("-l", "NonSandboxTest"), Tests.Argument("-n", "SandboxTest"), Tests.Filter(sandboxFilter)),
      unmanagedSourceDirectories in SandboxTest <<= (baseDirectory in SandboxTest) (base => Seq(base / "test")),
      unmanagedResourceDirectories in SandboxTest <<= (baseDirectory in SandboxTest) (base => Seq(base / "test")),
      unmanagedResourceDirectories in SandboxTest <+= baseDirectory(_ / "target/web/public/test"),
      Keys.fork in SandboxTest := false,
      parallelExecution in SandboxTest := false,
      addTestReportOption(SandboxTest, "sandbox-test-reports")
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
lazy val plugins: Seq[Plugins] = Seq(
  SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin
)
lazy val acceptanceTestDeps: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "acceptance",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "acceptance",
  "org.pegdown" % "pegdown" % "1.6.0" % "acceptance",
  "org.jsoup" % "jsoup" % "1.10.2" % "acceptance",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "acceptance",
  "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % "acceptance",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "acceptance",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % "acceptance",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % "acceptance",
  "org.mockito" % "mockito-all" % "1.10.19" % "acceptance",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "acceptance"
).map(
  _.excludeAll(
    ExclusionRule(organization = "commons-logging"),
    ExclusionRule(organization = "log4j"),
    ExclusionRule(organization = "org.apache.logging.log4j"),
    ExclusionRule("org.apache.logging.log4j", "log4j-core"),
    ExclusionRule("org.slf4j", "slf4j-log412"),
    ExclusionRule("org.slf4j", "slf4j-jdk14")
  ))
lazy val testDeps: Seq[ModuleID] = Seq(
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test",
  "org.jsoup" % "jsoup" % "1.10.2" % "test",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test",
  "uk.gov.hmrc" %% "hmrctest" % "2.3.0" % "test",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % "test",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0" % "test",
  "org.mockito" % "mockito-all" % "1.10.19" % "test",
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"
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
  "uk.gov.hmrc" %% "frontend-bootstrap" % "8.18.0",
  "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
  "uk.gov.hmrc" %% "json-encryption" % "3.2.0",
  "uk.gov.hmrc" %% "play-json-union-formatter" % "1.3.0",
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


def sandboxFilter(name: String): Boolean = !unitFilter(name)

def unitFilter(name: String): Boolean = !acceptanceFilter(name)

def acceptanceFilter(name: String): Boolean = name startsWith "acceptance"

def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
  tests map {
    test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
  }

coverageMinimum := 55
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;testOnlyDoNotUseInAppConf.*;app.*;uk.gov.hmrc.BuildInfo"
