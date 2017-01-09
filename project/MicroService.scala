import play.routes.compiler.StaticRoutesGenerator
import play.sbt.routes.RoutesKeys.routesGenerator
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._

import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.Import.WebKeys._
import net.ground5hark.sbt.concat.Import._
import com.typesafe.sbt.uglify.Import._
import com.typesafe.sbt.digest.Import._

trait MicroService {

  import uk.gov.hmrc._
  import DefaultBuildSettings._
  import TestPhases._

  val appName: String
  val appDependencies : Seq[ModuleID]

  lazy val plugins : Seq[Plugins] = Seq.empty
  lazy val playSettings : Seq[Setting[_]] = Seq.empty

  def unitFilter(name: String): Boolean = !acceptanceFilter(name)

  def acceptanceFilter(name: String): Boolean = name startsWith "acceptance"


  lazy val microservice = Project(appName, file("."))
    .enablePlugins(Seq(play.sbt.PlayScala) ++ plugins : _*)
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
    .settings(playSettings : _*)
    .settings(scalaSettings: _*)
    .settings(publishingSettings: _*)
    .settings(defaultSettings(): _*)
    .settings(
      targetJvm := "jvm-1.8",
      scalaVersion := "2.11.7",
      libraryDependencies ++= appDependencies,
      parallelExecution in Test := false,
      fork in Test := false,
      retrieveManaged := true,
      evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
      routesGenerator := StaticRoutesGenerator
    )
    .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
    .settings(testOptions in Test := Seq(Tests.Filter(unitFilter)),
      addTestReportOption(Test, "test-reports"),
      unmanagedSourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest)(base => Seq(base / "test/unit")),
      unmanagedResourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest)(base => Seq(base / "test/unit"))
    )
    .configs(AcceptanceTest)
    .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
    .settings(
      testOptions in AcceptanceTest := Seq(Tests.Filter(acceptanceFilter)),
      unmanagedSourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest)(base => Seq(base / "test")),
      unmanagedResourceDirectories in AcceptanceTest <<= (baseDirectory in AcceptanceTest)(base => Seq(base / "test")),
      unmanagedResourceDirectories in AcceptanceTest <+=  baseDirectory ( _ /"target/web/public/test" ),
      Keys.fork in AcceptanceTest := false,
      parallelExecution in AcceptanceTest := false,
      addTestReportOption(AcceptanceTest, "acceptance-test-reports")
    )
    .settings(
      resolvers := Seq(
        Resolver.bintrayRepo("hmrc", "releases"),
        Resolver.typesafeRepo("releases"),
        Resolver.jcenterRepo)
    )
    .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
}

private object TestPhases {

  val allPhases = "tt->test;test->test;test->compile;compile->compile"
  val allItPhases = "tit->it;it->it;it->compile;compile->compile"

  lazy val TemplateTest = config("tt") extend Test
  lazy val TemplateItTest = config("tit") extend IntegrationTest
  lazy val AcceptanceTest = config("acceptance") extend Test

  def oneForkedJvmPerTest(tests: Seq[TestDefinition]) =
    tests map {
      test => new Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    }
}

