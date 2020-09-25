package utils

import connectors.WiremockSugarIt
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatest.WordSpec
import org.scalatest.Matchers
import play.api.test.FutureAwaits
import play.api.test.DefaultAwaitTimeout

trait WiremockSpec
  extends WordSpec
  with Matchers
  with GuiceOneServerPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout
  with WiremockSugarIt
