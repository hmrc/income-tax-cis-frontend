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

package controllers

import controllers.routes.{ContractorCYAController, MaterialsAmountController}
import forms.{FormsProvider, YesNoForm}
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
import support.mocks.{MockActionsProvider, MockMaterialsService}
import views.html.MaterialsView

import java.time.Month.MAY

class MaterialsControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockMaterialsService {

  private val underTest = new MaterialsController(
    mockActionsProvider,
    new FormsProvider(),
    inject[MaterialsView],
    mockMaterialsService,
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

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "")
      val result = underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = "some-ref")(request)

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select(".govuk-error-summary").isEmpty shouldBe false
    }

    "handle internal server error when save operation fails with database error" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = MAY.toString.toLowerCase, aCisUserData.employerRef)
      mockSaveQuestion(aUser, aCisUserData, questionValue = true, result = Left(DataNotFoundError))
      mockInternalServerError(InternalServerError)

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")
      val result = underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef)(request)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to Materials amount page when Yes submitted successfully and not finished" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = MAY.toString.toLowerCase, employerRef = aCisUserData.employerRef)
      mockSaveQuestion(aUser, aCisUserData, questionValue = true, result = Right(aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None))))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")
      await(underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef)(request)) shouldBe
        Redirect(MaterialsAmountController.show(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef))
    }

    "redirect to Check CIS Deductions Page when Yes submitted successfully and is finished" in {
      mockEndOfYearWithSessionData(taxYearEOY, month = MAY.toString.toLowerCase, employerRef = aCisUserData.employerRef)
      mockSaveQuestion(aUser, aCisUserData, questionValue = true, result = Right(aCisUserData))

      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "true")
      await(underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef).apply(request)) shouldBe
        Redirect(ContractorCYAController.show(taxYearEOY, aPeriodData.deductionPeriod.toString.toLowerCase, aCisDeductions.employerRef))
    }

    "redirect to Check CIS Deductions Page when No submitted successfully" in {
      val request = fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      mockEndOfYearWithSessionData(taxYearEOY, month = "may", employerRef = aCisUserData.employerRef)
      mockSaveQuestion(aUser, aCisUserData, questionValue = false, result = Right(aCisUserData))

      await(underTest.submit(taxYearEOY, MAY.toString.toLowerCase, contractor = aCisUserData.employerRef).apply(request)) shouldBe
        Redirect(ContractorCYAController.show(taxYearEOY, aPeriodData.deductionPeriod.toString.toLowerCase, aCisDeductions.employerRef))
    }
  }
}
