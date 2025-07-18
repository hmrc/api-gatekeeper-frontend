import sbt._

object AppDependencies {

  lazy val jsoupVersion      = "1.12.1"
  lazy val scalaCheckVersion = "1.17.0"
  lazy val bootstrapVersion  = "9.13.0"
  lazy val playFrontendVersion = "12.6.0"

  val tpdDomainVersion    = "0.13.0"
  val apiDomainVersion    = "0.19.1"
  val appDomainVersion    = "0.81.0"

  def apply(): Seq[ModuleID] = dependencies ++ testDependencies

  lazy val dependencies = Seq(
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30"            % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30"            % playFrontendVersion,
    "uk.gov.hmrc"       %% "play-conditional-form-mapping-play-30" % "3.3.0",
    "uk.gov.hmrc"       %% "crypto-json-play-30"                   % "8.2.0",
    "commons-net"        % "commons-net"                           % "3.9.0",
    "org.apache.commons" % "commons-csv"                           % "1.10.0",
    "uk.gov.hmrc"       %% "internal-auth-client-play-30"          % "3.1.0",
    "uk.gov.hmrc"       %% "api-platform-application-domain"       % appDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-api-domain"               % apiDomainVersion,
    "uk.gov.hmrc"       %% "api-platform-tpd-domain"               % tpdDomainVersion
  )

  lazy val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"                    % bootstrapVersion,
    "org.jsoup"               % "jsoup"                                     % jsoupVersion,
    "uk.gov.hmrc"            %% "ui-test-runner"                            % "0.46.0",
    "org.mockito"            %% "mockito-scala-scalatest"                   % "1.17.30",
    "org.scalacheck"         %% "scalacheck"                                % scalaCheckVersion,
    "uk.gov.hmrc"            %% "api-platform-test-tpd-domain"              % tpdDomainVersion,
    "uk.gov.hmrc"            %% "api-platform-application-domain-fixtures"  % appDomainVersion
  ).map(_ % "test")
}
