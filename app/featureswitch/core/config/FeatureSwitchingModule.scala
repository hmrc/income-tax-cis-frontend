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

import featureswitch.core.models.FeatureSwitch
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

import javax.inject.Singleton

@Singleton
class FeatureSwitchingModule extends Module with FeatureSwitchRegistry {

  val switches: Seq[FeatureSwitch] = Seq(
    AlwaysEOY,
    TaxYearError,
    WelshToggle,
    Tailoring,
    SectionCompletedQuestion,
    UseEncryption,
    EmaSupportingAgent
  )

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[FeatureSwitchRegistry].to(this).eagerly()
    )
  }
}

case object AlwaysEOY extends FeatureSwitch {
  override val configName: String = prefix + "alwaysEOY"
  override val displayName: String = "Always EOY enabled"
}

case object TaxYearError extends FeatureSwitch {
  override val configName: String = prefix + "taxYearErrorFeatureSwitch"
  override val displayName: String = "Tax Tear Error enabled"
}

case object WelshToggle extends FeatureSwitch {
  override val configName: String = prefix + "welshToggleEnabled"
  override val displayName: String = "Welsh Translation enabled"
}

case object Tailoring extends FeatureSwitch {
  override val configName: String = prefix + "tailoringEnabled"
  override val displayName: String = "Tailoring enabled"
}

case object SectionCompletedQuestion extends FeatureSwitch {
  override val configName: String = prefix + "sectionCompletedQuestionEnabled"
  override val displayName: String = "Section Completed Question enabled"
}

case object UseEncryption extends FeatureSwitch {
  override val configName: String = prefix + "useEncryption"
  override val displayName: String = "Use Encryption enabled"
}

case object EmaSupportingAgent extends FeatureSwitch {
  override val configName: String = prefix + "ema-supporting-agents-enabled"
  override val displayName: String = "EMA Support Agents enabled"
}

