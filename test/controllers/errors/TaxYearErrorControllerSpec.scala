/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.errors

import common.SessionValues
import common.SessionValues.VALID_TAX_YEARS
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentType, status}
import play.api.test.{FakeRequest, Helpers}
import support.builders.models.UserBuilder.aUser
import support.mocks.MockAuthorisedAction
import support.{ControllerUnitTest, TaxYearProvider}
import views.html.templates.TaxYearErrorTemplate

class TaxYearErrorControllerSpec extends ControllerUnitTest with MockAuthorisedAction with TaxYearProvider {
  private lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()
  private lazy val taxYearErrorTemplate: TaxYearErrorTemplate = app.injector.instanceOf[TaxYearErrorTemplate]

  private lazy val underTest = new TaxYearErrorController(mockAuthorisedAction, mockMessagesControllerComponents, appConfig, taxYearErrorTemplate)

  ".show()" should {
    "return an OK response when .show() is called and user is authenticated as an individual" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val fakeRequest = FakeRequest("GET", "/error/wrong-tax-year")
        .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)
        .withSession(VALID_TAX_YEARS -> validTaxYearList.mkString(","))

      val result = underTest.show()(fakeRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    "return an OK response when .show() is called and user is authenticated as an agent" in {
      mockAuthAsAgent()

      val fakeRequest = FakeRequest("GET", "/error/wrong-tax-year")
        .withHeaders("X-Session-ID" -> aUser.sessionId)
        .withSession(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
        )

      val result = underTest.show()(fakeRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    "return a SEE_OTHER response when .show() is called and user isn't authenticated" in {
      mockFailToAuthenticate()

      val fakeRequest = FakeRequest("GET", "/error/wrong-tax-year")
        .withHeaders(newHeaders = "X-Session-ID" -> aUser.sessionId)
        .withSession(VALID_TAX_YEARS -> validTaxYearList.mkString(","))

      val result = underTest.show()(fakeRequest)

      status(result) shouldBe SEE_OTHER
      await(result).header.headers.getOrElse("Location", "/") shouldBe "/update-and-submit-income-tax-return/construction-industry-scheme-deductions/error/not-authorised-to-use-service"
    }
  }
}
