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

package controllers

import java.time.Month

import controllers.routes.DeductionAmountController
import forms.FormsProvider
import models.mongo.DataNotFoundError
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentAsString, contentType, status}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockLabourPayService}
import views.html.LabourPayView

class LabourPayControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockLabourPayService {

  private val underTest = new LabourPayController(
    mockActionsProvider,
    new FormsProvider(),
    inject[LabourPayView],
    mockLabourPayService,
    mockErrorHandler
  )

  ".show" should {
    "return successful response" in {
      mockEndOfYearWithSessionData(taxYearEOY, "some-ref")

      val result = underTest.show(taxYearEOY, month = "may", contractor = "some-ref").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "render page with error when validation of form fails" in {
      mockEndOfYearWithSessionData(taxYearEOY, employerRef = "some-ref")

      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "2.3.4"))

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-title").isEmpty shouldBe false
    }

    "handle internal server error when save operation fails with database error" in {
      mockEndOfYearWithSessionData(taxYearEOY, aCisUserData.employerRef)
      mockSaveLabourPay(aUser, aCisUserData, amount = 123, result = Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      val result = underTest.submit(taxYearEOY, Month.MAY.toString, aCisUserData.employerRef).apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "123"))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to Deductions amount page on successful submission" in {
      mockEndOfYearWithSessionData(taxYearEOY, aCisUserData.employerRef)
      mockSaveLabourPay(aUser, aCisUserData, amount = 123, result = Right(()))

      await(underTest.submit(taxYearEOY, Month.MAY.toString, aCisUserData.employerRef).apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "123"))) shouldBe
        Redirect(DeductionAmountController.show(taxYearEOY, Month.MAY.toString, aCisUserData.employerRef))
    }
  }
}
