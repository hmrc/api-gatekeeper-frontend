package testdata

import model.{Access, Standard}

trait AccessTestData {

  val standardAccess: Access = Standard(
    redirectUris = List("http://localhost:8080/callback"),
    termsAndConditionsUrl = Some("http://localhost:22222/terms"),
    privacyPolicyUrl = Some("http://localhost:22222/privacy")
  )
}
