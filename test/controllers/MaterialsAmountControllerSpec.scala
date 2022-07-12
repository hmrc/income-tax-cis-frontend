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

import controllers.routes.ContractorCYAController
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
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockMaterialsService}
import views.html.MaterialsAmountView

import java.time.Month

class MaterialsAmountControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockMaterialsService {

  private val underTest = new MaterialsAmountController(
    mockActionsProvider, new FormsProvider(),
    inject[MaterialsAmountView],
    mockMaterialsService,
    mockErrorHandler
  )

  ".show" should {
    "redirect to income tax submission overview when costOfMaterialsQuestion is None" in {
      val periodData = aCYAPeriodData.copy(costOfMaterialsQuestion = None)
      mockEndOfYearWithSessionData(taxYear = taxYearEOY, month = Month.MAY.toString, aCisUserData.copy(employerRef = "some-ref", cis = aCisCYAModel.copy(periodData = Some(periodData))))

      await(underTest.show(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "redirect to income tax submission overview when costOfMaterialsQuestion is false" in {
      val periodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false))
      mockEndOfYearWithSessionData(taxYear = taxYearEOY, month = Month.MAY.toString, aCisUserData.copy(employerRef = "some-ref", cis = aCisCYAModel.copy(periodData = Some(periodData))))

      await(underTest.show(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return a successful response when costOfMaterialsQuestion is true" in {
      val periodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(true))
      mockEndOfYearWithSessionData(taxYear = taxYearEOY, month = Month.MAY.toString, aCisUserData.copy(employerRef = "some-ref", cis = aCisCYAModel.copy(periodData = Some(periodData))))

      val result = underTest.show(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "redirect to income tax submission overview when costOfMaterialsQuestion is None" in {
      val periodData = aCYAPeriodData.copy(costOfMaterialsQuestion = None)
      mockEndOfYearWithSessionData(taxYear = taxYearEOY, month = Month.MAY.toString, aCisUserData.copy(employerRef = "some-ref", cis = aCisCYAModel.copy(periodData = Some(periodData))))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "2.3.4")
      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(request)

      await(result) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "redirect to income tax submission overview when costOfMaterialsQuestion is false" in {
      val periodData = aCYAPeriodData.copy(costOfMaterialsQuestion = Some(false))
      mockEndOfYearWithSessionData(taxYear = taxYearEOY, month = Month.MAY.toString, aCisUserData.copy(employerRef = "some-ref", cis = aCisCYAModel.copy(periodData = Some(periodData))))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "2.3.4")
      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(request)

      await(result) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "render page with error when validation of form fails" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = Month.MAY.toString, employerRef = "some-ref")

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "2.3.4")
      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-title").isEmpty shouldBe false
    }

    "handle internal server error when save operation fails with database error" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = Month.MAY.toString, aCisUserData.employerRef)
      mockSaveAmount(aUser, aCisUserData, amount = 123, result = Left(DataNotFoundError))
      mockInternalServerError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "123")
      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = aCisUserData.employerRef).apply(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to Income Tax Submission Overview page on successful submission" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = Month.MAY.toString, employerRef = aCisUserData.employerRef)
      mockSaveAmount(aUser, aCisUserData, amount = 123, result = Right(()))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(AmountForm.amount -> "123")
      await(underTest.submit(taxYearEOY, Month.MAY.toString, contractor = aCisUserData.employerRef).apply(request)) shouldBe
        Redirect(ContractorCYAController.show(taxYearEOY, aPeriodData.deductionPeriod.toString.toLowerCase, aCisDeductions.employerRef))
    }
  }
}
