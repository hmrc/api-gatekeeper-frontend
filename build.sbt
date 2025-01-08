import com.typesafe.sbt.digest.Import._
import net.ground5hark.sbt.concat.Import._
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings._

lazy val appName = "api-gatekeeper-frontend"

Global / bloopAggregateSourceDependencies := true
Global / bloopExportJarClassifiers := Some(Set("sources"))

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 0
ThisBuild / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(concat)
  )
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    shellPrompt := (_ => "> ")
  )
  .settings(
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testCommon"
  )
  .settings(ScoverageSettings())
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.gatekeeper.controllers.binders._",
      "uk.gov.hmrc.gatekeeper.models._",
      "uk.gov.hmrc.apiplatform.modules.apis.domain.models._"
    )
  )
  .settings(
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
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
      // suppress warnings in generated routes files
      "-Wconf:src=routes/.*:s"
    )
  )

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    name := "integration-tests",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    DefaultBuildSettings.itSettings()
  )

lazy val acceptance = (project in file("acceptance"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    name := "acceptance-tests",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    DefaultBuildSettings.itSettings(),
    addTestReportOption(Test, "acceptance-reports")
  )


commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "it/clean" :: "acceptance/clean" :: state },
  Command.command("fmtAll") { state => "scalafmtAll" :: "it/scalafmtAll" :: "acceptance/scalafmtAll" :: state },
  Command.command("fixAll") { state => "scalafixAll" :: "it/scalafixAll" :: "acceptance/scalafixAll" :: state },
  Command.command("testAll") { state => "test" :: "it/test" :: "acceptance/test" :: state },

  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "testAll" :: "coverageOff" :: "coverageAggregate" :: state }
)
