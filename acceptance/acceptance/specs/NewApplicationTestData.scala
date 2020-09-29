package acceptance.specs

import builder.{ApplicationBuilder, SubscriptionsBuilder}
import model._
import play.api.libs.json.Json
import acceptance.mocks.{ApplicationStateMock, CollaboratorsMock, TestData, AccessMock, ApplicationResponseMock}
import acceptance.mocks.StateHistoryMock

trait NewApplicationTestData extends SubscriptionsBuilder with ApplicationBuilder with CollaboratorsMock with ApplicationStateMock with TestData with StateHistoryMock with AccessMock with ApplicationResponseMock {

  val testIpWhitelist = Set.empty[String]

  val newApplicationUser = Json.toJson(unverifiedUser).toString

   val applicationResponseForNewApplicationUserEmail = Json.toJson(Seq(applicationResponseTest)).toString

   val applicationResponseForNewApplicationTest = ApplicationWithHistory(applicationResponseTest, stateHistories)

    val applicationResponseForNewApplication = Json.toJson(applicationResponseForNewApplicationTest).toString
 }
