package common

import org.openqa.selenium.WebDriver
import play.api.Application
import utils.MockCookies

trait CookieHelper {
  def addSessionCookie()(implicit webDriver: WebDriver, app: Application) = {
    webDriver.manage().addCookie(MockCookies.makeSeleniumCookie(app))
  }
}
