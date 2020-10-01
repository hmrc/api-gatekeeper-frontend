package acceptance.mocks

import model.{Access, Standard}

trait AccessMock {

  val standardAccess: Access = Standard(
    redirectUris = Seq("http://localhost:8080/callback"),
    termsAndConditionsUrl = Some("http://localhost:22222/terms"),
    privacyPolicyUrl = Some("http://localhost:22222/privacy")
  )
}
