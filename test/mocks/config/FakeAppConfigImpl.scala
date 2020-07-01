package mocks.config

import com.google.inject.Singleton
import config.AppConfigImpl
import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.RunMode

@Singleton
class FakeAppConfigImpl @Inject()(config: Configuration, runMode: RunMode)
  extends AppConfigImpl(config, runMode) {

  override def title = "Unit Test Title"
}
