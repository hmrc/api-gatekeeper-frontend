import play.core.PlayVersion
import sbt._

object AppDependencies {
  
  lazy val slf4jVersion = "1.7.23"
  lazy val logbackVersion = "1.1.10"
  lazy val jsoupVersion = "1.12.1"
  lazy val scalaCheckVersion = "1.15.4"
  lazy val enumeratumVersion = "1.6.2"
  lazy val bootstrapVersion = "7.12.0"
  lazy val seleniumVersion = "4.4.0"

  def apply(): Seq[ModuleID] = dependencies ++ testDependencies

  lazy val dependencies = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-frontend-play-28"        % bootstrapVersion,
    "uk.gov.hmrc"                 %% "play-frontend-hmrc"                % "5.3.0-play-28",
    "uk.gov.hmrc"                 %% "play-conditional-form-mapping"     % "1.12.0-play-28",
    "uk.gov.hmrc"                 %% "json-encryption"                   % "5.1.0-play-28",
    "uk.gov.hmrc"                 %% "emailaddress"                      % "3.7.0",
    "commons-net"                 %  "commons-net"                       % "3.6",
    "org.slf4j"                   %  "slf4j-api"                         % slf4jVersion,
    "org.slf4j"                   %  "jcl-over-slf4j"                    % slf4jVersion,
    "org.slf4j"                   %  "log4j-over-slf4j"                  % slf4jVersion,
    "org.slf4j"                   %  "jul-to-slf4j"                      % slf4jVersion,
    "ch.qos.logback"              %  "logback-classic"                   % logbackVersion,
    "ch.qos.logback"              %  "logback-core"                      % logbackVersion,
    "com.typesafe.play"           %% "play-json"                         % "2.9.2",
    "org.typelevel"               %% "cats-core"                         % "2.3.1",
    "com.beachape"                %% "enumeratum-play-json"              % enumeratumVersion,
    "org.apache.commons"          %  "commons-csv"                       % "1.9.0",
    "uk.gov.hmrc"                 %% "internal-auth-client-play-28"      % "1.2.0",
    "uk.gov.hmrc"                 %% "api-platform-application-events"   % "0.15.0",
    "uk.gov.hmrc"                 %% "api-platform-application-commands" % "0.7.0"
  )

  lazy val testScopes = Seq(Test.name, IntegrationTest.name, "acceptance").mkString(",")

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"                 %% "bootstrap-test-play-28"            % bootstrapVersion,
    "org.jsoup"                   %  "jsoup"                             % jsoupVersion,
    "com.github.tomakehurst"      %  "wiremock-jre8-standalone"          % "2.34.0",
    "org.seleniumhq.selenium"     %  "selenium-java"                     % seleniumVersion,
    "org.seleniumhq.selenium"     %  "htmlunit-driver"                   % "3.64.0",
    "org.mockito"                 %% "mockito-scala-scalatest"           % "1.17.12",
    "org.scalacheck"              %% "scalacheck"                        % scalaCheckVersion,
    "org.scalatestplus.play"      %% "scalatestplus-play"                % "5.1.0",
    "uk.gov.hmrc"                 %% "webdriver-factory"                 % "0.40.0"
  ).map (_ % testScopes)
}
