package utils

import connectors.WiremockSugarIt
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite

import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

trait WiremockSpec
  extends WordSpec
  with Matchers
  with GuiceOneServerPerSuite
  with FutureAwaits
  with DefaultAwaitTimeout
  with WiremockSugarIt
