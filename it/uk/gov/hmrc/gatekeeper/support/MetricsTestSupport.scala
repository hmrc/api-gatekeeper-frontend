package uk.gov.hmrc.gatekeeper.support

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import org.scalatest.Suite
import play.api.Application

import scala.collection.JavaConverters
import org.scalatest.matchers.should.Matchers

trait MetricsTestSupport {
  self: Suite with Matchers =>

  def app: Application

  private var metricsRegistry: MetricRegistry = _

  def givenCleanMetricRegistry(): Unit = {
    val registry = app.injector.instanceOf[Metrics].defaultRegistry
    for (metric <- JavaConverters
                    .asScalaIterator[String](registry.getMetrics.keySet().iterator())) {
      registry.remove(metric)
    }
    metricsRegistry = registry
  }

  def verifyTimerExistsAndBeenUpdated(metric: String): Unit = {
    val timers = metricsRegistry.getTimers
    val metrics = timers.get(s"Timer-$metric")
    if (metrics == null) {
      throw new Exception(s"Metric [$metric] not found, try one of ${timers.keySet()}")
    }
    metrics.getCount >= 1L shouldBe true
  }
}
