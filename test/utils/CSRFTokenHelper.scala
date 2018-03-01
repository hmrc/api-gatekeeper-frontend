package utils

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.filters.csrf.CSRF.Token

object CSRFTokenHelper {
  implicit class CSRFRequestHeader(request: FakeRequest[AnyContentAsEmpty.type]) {
    def withCSRFToken: FakeRequest[AnyContentAsEmpty.type] = request.copyFakeRequest(tags = request.tags ++ Map(
      Token.NameRequestTag -> "test",
      Token.RequestTag -> "test"
    ))
  }
}
