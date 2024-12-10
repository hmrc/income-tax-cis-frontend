/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package featureswitch.core.config

import config.AppConfig
import featureswitch.core.models.FeatureSwitch
import play.api.Logging

trait FeatureSwitching extends Logging { config: AppConfig =>

  def isEnabled(featureSwitch: FeatureSwitch): Boolean =
    sys.props get featureSwitch.configName match {
      case Some(value) => value.toBoolean
      case None => config.getFeatureSwitchValue(featureSwitch.configName)
    }

  def enable(featureSwitch: FeatureSwitch): Unit = {
    logger.warn(s"[enable] $featureSwitch")
    sys.props += featureSwitch.configName -> true.toString
  }

  def disable(featureSwitch: FeatureSwitch): Unit = {
    logger.warn(s"[disable] $featureSwitch")
    sys.props += featureSwitch.configName -> false.toString
  }
}
