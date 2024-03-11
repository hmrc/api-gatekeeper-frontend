import sbt._

object AppDependencies {

  lazy val slf4jVersion      = "1.7.23"
  lazy val logbackVersion    = "1.1.10"
  lazy val jsoupVersion      = "1.12.1"
  lazy val scalaCheckVersion = "1.17.0"
  lazy val bootstrapVersion  = "8.4.0"
  lazy val seleniumVersion   = "4.4.0"

  val apiDomainVersion    = "0.15.0"
  val commonDomainVersion = "0.13.0"
  val appDomainVersion    = "0.40.0"

  def apply(): Seq[ModuleID] = dependencies ++ testDependencies

  lazy val dependencies = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"            % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"            % "8.5.0",
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30" % "2.0.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"                   % "7.6.0",
    "uk.gov.hmrc"       %% "emailaddress"                          % "3.8.0",
    "commons-net"        % "commons-net"                           % "3.9.0",
    "org.apache.commons" % "commons-csv"                           % "1.10.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"          % "1.10.0",
    "uk.gov.hmrc"       %% "api-platform-application-domain"       % appDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-api-domain"               % apiDomainVersion
  )

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"          % bootstrapVersion,
    "org.jsoup"               % "jsoup"                           % jsoupVersion,
    "uk.gov.hmrc"            %% "ui-test-runner"                  % "0.16.0",
    "org.mockito"            %% "mockito-scala-scalatest"         % "1.17.30",
    "org.scalacheck"         %% "scalacheck"                      % scalaCheckVersion,
    "uk.gov.hmrc"            %% "api-platform-test-common-domain" % commonDomainVersion
  ).map(_ % "test")
}
