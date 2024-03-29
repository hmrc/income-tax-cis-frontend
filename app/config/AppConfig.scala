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

package config

import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig) {

  val incomeTaxSubmissionUrlKey = "microservice.services.income-tax-submission.url"
  val contactFrontendUrlKey = "microservice.services.contact-frontend.url"
  val incomeTaxSubmissionFrontendUrlKey = "microservice.services.income-tax-submission-frontend.url"
  val basGatewayFrontendUrlKey = "microservice.services.bas-gateway-frontend.url"
  val feedbackFrontendUrlKey = "microservice.services.feedback-frontend.url"
  val viewAndChangeUrlKey = "microservice.services.view-and-change.url"
  val incomeTaxCISUrlKey = "microservice.services.income-tax-cis.url"
  val signInContinueUrlKey = "microservice.services.sign-in.continueUrl"

  private lazy val signInBaseUrl: String = servicesConfig.getString("microservice.services.sign-in.url")
  private lazy val signInContinueBaseUrl: String = servicesConfig.getString(signInContinueUrlKey)
  lazy val signInContinueUrlRedirect: String = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl

  private lazy val signInOrigin = servicesConfig.getString("appName")
  lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrlRedirect&origin=$signInOrigin"

  def defaultTaxYear: Int = servicesConfig.getInt("defaultTaxYear")
  lazy val alwaysEOY: Boolean = servicesConfig.getBoolean("alwaysEOY")

  lazy val incomeTaxSubmissionBEBaseUrl: String = servicesConfig.getString(incomeTaxSubmissionUrlKey) + "/income-tax-submission-service"

  def incomeTaxSubmissionBaseUrl: String = servicesConfig.getString(incomeTaxSubmissionFrontendUrlKey) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context")

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")
  def incomeTaxSubmissionStartUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    "/start"
  def incomeTaxSubmissionIvRedirect: String = incomeTaxSubmissionBaseUrl +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")

  lazy val incomeTaxCISBEUrl: String = s"${servicesConfig.getString(incomeTaxCISUrlKey)}/income-tax-cis"

  private lazy val vcBaseUrl: String = servicesConfig.getString(viewAndChangeUrlKey)
  def viewAndChangeEnterUtrUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents/client-utr"

  lazy private val appUrl: String = servicesConfig.getString("microservice.url")
  lazy private val contactFrontEndUrl = servicesConfig.getString(contactFrontendUrlKey)

  lazy private val contactFormServiceIndividual = "update-and-submit-income-tax-return"
  lazy private val contactFormServiceAgent = "update-and-submit-income-tax-return-agent"
  def contactFormServiceIdentifier(implicit isAgent: Boolean): String = if(isAgent) contactFormServiceAgent else contactFormServiceIndividual

  private def requestUri(implicit request: RequestHeader): String = SafeRedirectUrl(appUrl + request.uri).encodedUrl

  private lazy val feedbackFrontendUrl = servicesConfig.getString(feedbackFrontendUrlKey)

  def feedbackSurveyUrl(implicit isAgent: Boolean): String = s"$feedbackFrontendUrl/feedback/$contactFormServiceIdentifier"

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String =
    s"$contactFrontEndUrl/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=$requestUri"

  def contactUrl(implicit isAgent: Boolean): String = s"$contactFrontEndUrl/contact/contact-hmrc?service=$contactFormServiceIdentifier"

  private lazy val basGatewayUrl = servicesConfig.getString(basGatewayFrontendUrlKey)

  lazy val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

  lazy val timeoutDialogTimeout: Int = servicesConfig.getInt("timeoutDialogTimeout")
  lazy val timeoutDialogCountdown: Int = servicesConfig.getInt("timeoutDialogCountdown")

  //Mongo config
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  lazy val mongoTTL: Int = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  def taxYearErrorFeature: Boolean = servicesConfig.getBoolean("taxYearErrorFeatureSwitch")

  def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  lazy val welshToggleEnabled: Boolean = servicesConfig.getBoolean("feature-switch.welshToggleEnabled")

  lazy val tailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoringEnabled")

  lazy val useEncryption: Boolean = servicesConfig.getBoolean("useEncryption")
}
