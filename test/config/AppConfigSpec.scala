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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mock, when}
import support.{FakeRequestHelper, UnitTest}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URLEncoder

class AppConfigSpec extends UnitTest
  with FakeRequestHelper {

  private val mockServicesConfig: ServicesConfig = mock(classOf[ServicesConfig])
  private val appUrl = "http://localhost:9308"

  when(mockServicesConfig.getString("microservice.services.bas-gateway-frontend.url")).thenReturn("http://bas-gateway-frontend:9553")
  when(mockServicesConfig.getString("microservice.services.feedback-frontend.url")).thenReturn("http://feedback-frontend:9514")
  when(mockServicesConfig.getString("microservice.services.contact-frontend.url")).thenReturn("http://contact-frontend:9250")
  when(mockServicesConfig.getString("microservice.services.income-tax-submission.url")).thenReturn("http://income-tax-submission")
  when(mockServicesConfig.getString("microservice.services.income-tax-submission-frontend.url")).thenReturn("http://income-tax-submission-frontend")
  when(mockServicesConfig.getString("microservice.services.income-tax-submission-frontend.context")).thenReturn("/update-and-submit-income-tax-return")
  when(mockServicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")).thenReturn("/iv-uplift")
  when(mockServicesConfig.getString("microservice.services.view-and-change.url")).thenReturn("http://view-and-change")
  when(mockServicesConfig.getString("microservice.services.sign-in.url")).thenReturn("http://sign-in")
  when(mockServicesConfig.getString("microservice.services.sign-in.continueUrl")).thenReturn("http://sign-in-continue-url")
  when(mockServicesConfig.getString("microservice.url")).thenReturn(appUrl)
  when(mockServicesConfig.getString("appName")).thenReturn("income-tax-cis-frontend")

  private val underTest = new AppConfigImpl(mockServicesConfig)


  "AppConfig" should {
    "return correct feedbackUrl when the user is an individual" in {
      implicit val isAgent: Boolean = false
      val expectedBackUrl = URLEncoder.encode(appUrl + fakeIndividualRequest.uri, "UTf-8")
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
      underTest.viewAndChangeAgentsUrl shouldBe "http://view-and-change/report-quarterly/income-and-expenses/view/agents"
      underTest.incomeTaxSubmissionBEBaseUrl shouldBe "http://income-tax-submission/income-tax-submission-service"
      underTest.incomeTaxSubmissionIvRedirect shouldBe "http://income-tax-submission-frontend/update-and-submit-income-tax-return/iv-uplift"
    }

    "return the correct feedback url when the user is an agent" in {
      implicit val isAgent: Boolean = true
      val expectedBackUrl = URLEncoder.encode(appUrl + fakeAgentRequest.uri, "UTF-8")
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
