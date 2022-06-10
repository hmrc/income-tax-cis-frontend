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

import controllers.routes.{ContractorCYAController, MaterialsController}
import forms.{AmountForm, FormsProvider}
import models.mongo.DataNotFoundError
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentAsString, contentType, status}
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockDeductionAmountService}
import views.html.DeductionAmountView

import java.time.Month.MAY

class DeductionAmountControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockDeductionAmountService {

  private val underTest = new DeductionAmountController(
    mockActionsProvider,
    new FormsProvider(),
    inject[DeductionAmountView],
    mockDeductionAmountService,
    mockErrorHandler
  )

  ".show" should {
    "return successful response" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = "may", employerRef = "some-ref")

      val result = underTest.show(taxYearEOY, month = "may", contractor = "some-ref").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "render page with error when validation of form fails" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = "may", employerRef = "some-ref")

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "2.3.4")
      val result = underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = "some-ref").apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-title").isEmpty shouldBe false
    }

    "handle internal server error when save operation fails with database error" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = "may", aCisUserData.employerRef)
      mockSaveAmount(aUser, aCisUserData, amount = 123, result = Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "123")
      val result = underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef).apply(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to Materials Question Page on successful submission when data collection not finished" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = "may", employerRef = aCisUserData.employerRef)
      mockSaveAmount(aUser, aCisUserData, amount = 123, result = Right(aCisUserData.copy(cis = aCisCYAModel.copy(contractorName = None))))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "123")
      await(underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef).apply(request)) shouldBe
        Redirect(MaterialsController.show(taxYearEOY, MAY.toString.toLowerCase, aCisUserData.employerRef))
    }

    "redirect to Check CIS Deductions Page on successful submission when data collection finished" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = "may", employerRef = aCisUserData.employerRef)
      mockSaveAmount(aUser, aCisUserData, amount = 123, result = Right(aCisUserData))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "123")
      await(underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef).apply(request)) shouldBe
        Redirect(ContractorCYAController.show(taxYearEOY, aPeriodData.deductionPeriod.toString.toLowerCase, aCisDeductions.employerRef))
    }
  }
}
