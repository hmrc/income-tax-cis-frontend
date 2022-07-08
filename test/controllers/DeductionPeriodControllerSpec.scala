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

import common.SessionValues.TAX_YEAR
import controllers.routes.{ContractorCYAController, LabourPayController}
import forms.DeductionPeriodFormProvider
import models.mongo.DataNotUpdatedError
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{contentAsString, contentType, redirectLocation, status}
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockCISSessionService, MockDeductionPeriodService, MockErrorHandler}
import views.html.DeductionPeriodView

import java.time.Month
import java.time.Month.NOVEMBER

class DeductionPeriodControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockDeductionPeriodService
  with MockCISSessionService
  with MockErrorHandler {

  private val form = new DeductionPeriodFormProvider()
  private val pageView = inject[DeductionPeriodView]

  private val underTest = new DeductionPeriodController(
    mockActionsProvider,
    pageView,
    mockDeductionPeriodService,
    mockErrorHandler,
    form
  )(cc, ec, appConfig)

  ".show" should {
    "return successful response" in {
      mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, aCisUserData.copy(employerRef = aCisDeductions.employerRef))

      val result = underTest.show(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "render page with error when validation of form fails" in {
      val cisCYAModel = aCisCYAModel.copy(priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = NOVEMBER)))
      mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, aCisUserData.copy(employerRef = aCisDeductions.employerRef, cis = cisCYAModel))

      val request = fakeIndividualRequest.withMethod(POST.method).withSession(TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "november")
      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-title").isEmpty shouldBe false
    }

    "handle internal server error when save operation fails with database error" in {
      mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, aCisUserData.copy(employerRef = aCisDeductions.employerRef))
      mockSubmitMonth(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JUNE, Left(DataNotUpdatedError))
      mockInternalError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withSession(TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june")
      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "submit the month period without passed month" in {
      mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, aCisUserData.copy(employerRef = aCisDeductions.employerRef))
      mockSubmitMonth(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JUNE, Right(aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))))

      val request = fakeIndividualRequest.withMethod(POST.method).withSession(TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june")
      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef, month = None).apply(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe LabourPayController.show(taxYearEOY, Month.JUNE.toString.toLowerCase, aCisDeductions.employerRef).url
    }

    "submit month period when same month is passed and data collection is finished" in {
      mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, aCisUserData.copy(employerRef = aCisDeductions.employerRef), month = Some("june"))
      mockSubmitMonth(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JUNE, Right(aCisUserData))

      val request = fakeIndividualRequest.withMethod(POST.method).withSession(TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june")
      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef, Some("june")).apply(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe ContractorCYAController.show(taxYearEOY, Month.JUNE.toString.toLowerCase, aCisDeductions.employerRef).url
    }

    "submit month period when different month is passed and data not finished" in {
      mockEndOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, aCisUserData.copy(employerRef = aCisDeductions.employerRef), month = Some("july"))
      mockSubmitMonth(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JUNE, Right(aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))))

      val request = fakeIndividualRequest.withMethod(POST.method).withSession(TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june")
      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef, Some("july")).apply(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe LabourPayController.show(taxYearEOY, Month.JUNE.toString.toLowerCase, aCisDeductions.employerRef).url
    }
  }
}
