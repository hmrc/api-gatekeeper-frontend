package acceptance.mocks

import model.Access
import model.Standard

trait AccessMock {

  val testAccess: Access = Standard(
    redirectUris = Seq("http://localhost:8080/callback"),
    termsAndConditionsUrl = Some("http://localhost:22222/terms"),
    privacyPolicyUrl = Some("http://localhost:22222/privacy")
  )
}