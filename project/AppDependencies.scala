import play.core.PlayVersion
import sbt._

object AppDependencies {
  
  lazy val slf4jVersion = "1.7.23"
  lazy val logbackVersion = "1.1.10"
  lazy val jsoupVersion = "1.12.1"
  lazy val scalaCheckVersion = "1.14.0"
  lazy val enumeratumVersion = "1.6.2"
  lazy val bootstrapVersion = "5.24.0"

  def apply(): Seq[ModuleID] = dependencies ++ testDependencies

  lazy val dependencies = Seq(
    "uk.gov.hmrc"                 %%  "bootstrap-frontend-play-28"        % bootstrapVersion,
    "uk.gov.hmrc"                 %%  "time"                              % "3.25.0",
    "uk.gov.hmrc"                 %%  "play-ui"                           % "9.7.0-play-28",
    "uk.gov.hmrc"                 %%  "govuk-template"                    % "5.72.0-play-28",
    "uk.gov.hmrc"                 %%  "play-conditional-form-mapping"     % "1.9.0-play-28",
    "uk.gov.hmrc"                 %%  "json-encryption"                   % "4.10.0-play-28",
    "uk.gov.hmrc"                 %%  "play-json-union-formatter"         % "1.15.0-play-28",
    "uk.gov.hmrc"                 %%  "emailaddress"                      % "3.5.0",
    "uk.gov.hmrc"                 %% "play-frontend-hmrc"                 % "1.26.0-play-28",
    "commons-net"                 %  "commons-net"                        % "3.6",
    "org.slf4j"                   %  "slf4j-api"                          % slf4jVersion,
    "org.slf4j"                   %  "jcl-over-slf4j"                     % slf4jVersion,
    "org.slf4j"                   %  "log4j-over-slf4j"                   % slf4jVersion,
    "org.slf4j"                   %  "jul-to-slf4j"                       % slf4jVersion,
    "ch.qos.logback"              %  "logback-classic"                    % logbackVersion,
    "ch.qos.logback"              %  "logback-core"                       % logbackVersion,
    "com.typesafe.play"           %% "play-json"                          % "2.9.2",
    "com.typesafe.play"           %% "play-json-joda"                     % "2.9.2",
    "org.typelevel"               %% "cats-core"                          % "2.3.1",
    "com.beachape"                %% "enumeratum-play-json"               % enumeratumVersion,
    "org.apache.commons"          %  "commons-csv"                        % "1.9.0",
    "uk.gov.hmrc"                 %% "internal-auth-client-play-28"       % "1.2.0"
  )

  lazy val testScopes = Seq(Test.name, IntegrationTest.name, "acceptance").mkString(",")

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"             % bootstrapVersion,
    // "org.scalatestplus.play"  %%  "scalatestplus-play"        % "3.1.3",
    // "org.pegdown"             %   "pegdown"                   % "1.6.0",
    "org.jsoup"                   %   "jsoup"                             % jsoupVersion,
    // "com.typesafe.play"       %%  "play-test"                 % PlayVersion.current,
    "net.lightbody.bmp"           %   "browsermob-core"                   % "2.1.5",
    "com.github.tomakehurst"      %   "wiremock-jre8-standalone"          % "2.27.2",
    "org.seleniumhq.selenium"     %   "selenium-java"                     % "2.53.1",
    "org.seleniumhq.selenium"     %   "selenium-htmlunit-driver"          % "2.52.0",
    "org.mockito"                 %%  "mockito-scala-scalatest"           % "1.16.42",
    "org.scalacheck"              %%  "scalacheck"                        % scalaCheckVersion
  ).map (_ % testScopes)
}
