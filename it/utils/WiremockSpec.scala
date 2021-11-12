package utils

import connectors.WiremockSugarIt
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.FutureAwaits
import play.api.test.DefaultAwaitTimeout
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

trait WiremockSpec
  extends AnyWordSpec
  with Matchers
  with GuiceOneServerPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout
  with WiremockSugarIt
