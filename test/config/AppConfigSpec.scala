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

import org.scalamock.scalatest.MockFactory
import support.{FakeRequestHelper, UnitTest}
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigSpec extends UnitTest
  with MockFactory
  with FakeRequestHelper {

  private val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  private val appUrl = "http://localhost:9308"

  private val underTest = new AppConfig(mockServicesConfig)

  (mockServicesConfig.getString(_: String)).expects("microservice.services.bas-gateway-frontend.url").returns("http://bas-gateway-frontend:9553")

  (mockServicesConfig.getString(_: String)).expects("microservice.services.feedback-frontend.url").returns("http://feedback-frontend:9514")

  (mockServicesConfig.getString(_: String)).expects("microservice.services.contact-frontend.url").returns("http://contact-frontend:9250")
  (mockServicesConfig.getString(_: String)).expects("microservice.services.income-tax-submission.url").returns("http://income-tax-submission")
  (mockServicesConfig.getString(_: String)).expects("microservice.services.income-tax-submission-frontend.url").returns("http://income-tax-submission-frontend").twice()
  (mockServicesConfig.getString(_: String)).expects("microservice.services.income-tax-submission-frontend.context").returns("/update-and-submit-income-tax-return").twice()
  (mockServicesConfig.getString(_: String)).expects("microservice.services.income-tax-submission-frontend.iv-redirect").returns("/iv-uplift")
  (mockServicesConfig.getString(_: String)).expects("microservice.services.view-and-change.url").returns("http://view-and-change")
  (mockServicesConfig.getString(_: String)).expects("microservice.services.sign-in.url").returns("http://sign-in")
  (mockServicesConfig.getString(_: String)).expects("microservice.services.sign-in.continueUrl").returns("http://sign-in-continue-url")

  (mockServicesConfig.getString _).expects("microservice.url").returns(appUrl)
  (mockServicesConfig.getString _).expects("appName").returns("income-tax-cis-frontend")

  "AppConfig" should {
    "return correct feedbackUrl when the user is an individual" in {
      implicit val isAgent: Boolean = false
      val expectedBackUrl = SafeRedirectUrl(appUrl + fakeIndividualRequest.uri).encodedUrl
      val expectedServiceIdentifier = "update-and-submit-income-tax-return"
      val expectedBetaFeedbackUrl = s"http://contact-frontend:9250/contact/beta-feedback?service=$expectedServiceIdentifier&backUrl=$expectedBackUrl"
      val expectedFeedbackSurveyUrl = s"http://feedback-frontend:9514/feedback/$expectedServiceIdentifier"
      val expectedContactUrl = s"http://contact-frontend:9250/contact/contact-hmrc?service=$expectedServiceIdentifier"
      val expectedSignOutUrl = s"http://bas-gateway-frontend:9553/bas-gateway/sign-out-without-state"
      val expectedSignInUrl = "http://sign-in?continue=http%3A%2F%2Fsign-in-continue-url&origin=income-tax-cis-frontend"

      underTest.betaFeedbackUrl(fakeIndividualRequest, isAgent) shouldBe expectedBetaFeedbackUrl
      underTest.feedbackSurveyUrl shouldBe expectedFeedbackSurveyUrl
      underTest.contactUrl shouldBe expectedContactUrl
      underTest.signOutUrl shouldBe expectedSignOutUrl

      underTest.signInUrl shouldBe expectedSignInUrl

      underTest.incomeTaxSubmissionBaseUrl shouldBe "http://income-tax-submission-frontend/update-and-submit-income-tax-return"
      underTest.viewAndChangeEnterUtrUrl shouldBe "http://view-and-change/report-quarterly/income-and-expenses/view/agents/client-utr"
      underTest.incomeTaxSubmissionBEBaseUrl shouldBe "http://income-tax-submission/income-tax-submission-service"
      underTest.incomeTaxSubmissionIvRedirect shouldBe "http://income-tax-submission-frontend/update-and-submit-income-tax-return/iv-uplift"
    }

    "return the correct feedback url when the user is an agent" in {
      implicit val isAgent: Boolean = true
      val expectedBackUrl = SafeRedirectUrl(appUrl + fakeAgentRequest.uri).encodedUrl
      val expectedServiceIdentifierAgent = "update-and-submit-income-tax-return-agent"
      val expectedBetaFeedbackUrl = s"http://contact-frontend:9250/contact/beta-feedback?service=$expectedServiceIdentifierAgent&backUrl=$expectedBackUrl"
      val expectedFeedbackSurveyUrl = s"http://feedback-frontend:9514/feedback/$expectedServiceIdentifierAgent"
      val expectedContactUrl = s"http://contact-frontend:9250/contact/contact-hmrc?service=$expectedServiceIdentifierAgent"
      val expectedSignOutUrl = s"http://bas-gateway-frontend:9553/bas-gateway/sign-out-without-state"

      underTest.betaFeedbackUrl(fakeIndividualRequest, isAgent) shouldBe expectedBetaFeedbackUrl
      underTest.feedbackSurveyUrl shouldBe expectedFeedbackSurveyUrl
      underTest.contactUrl shouldBe expectedContactUrl
      underTest.signOutUrl shouldBe expectedSignOutUrl
    }
  }
}
