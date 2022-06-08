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

import common.SessionValues
import controllers.routes.{ContractorCYAController, DeductionPeriodController}
import forms.ContractorDetailsForm
import models.forms.ContractorDetails
import models.mongo.{CisCYAModel, DataNotFoundError}
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentAsString, contentType, status}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockContractorDetailsService, MockErrorHandler}
import views.html.ContractorDetailsView

class ContractorDetailsControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockContractorDetailsService
  with MockErrorHandler {

  private val pageView = inject[ContractorDetailsView]

  private val underTest = new ContractorDetailsController(
    mockActionsProvider,
    pageView,
    mockContractorDetailsService,
    mockErrorHandler
  )(cc, ec, appConfig)

  ".show" should {
    "return successful response when contractor not provided" in {
      mockNotInYear(taxYearEOY)

      val result = underTest.show(taxYearEOY, None).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    "return successful response when contractor provided" in {
      mockEndOfYearWithSessionData(taxYearEOY, aCisUserData.copy(employerRef = "contractor-ref"))

      val result = underTest.show(taxYearEOY, Some("contractor-ref")).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "return page with error when validation fails" in {
      mockNotInYear(taxYearEOY)

      val result = underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withFormUrlEncodedBody(ContractorDetailsForm.contractorName -> "some-name"))

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-title").isEmpty shouldBe false
    }

    "handle internal server error when save operation fails with database error when" when {
      "no contractor provided" in {
        mockNotInYear(taxYearEOY)
        mockSaveContractorDetails(taxYearEOY, aUser, None, ContractorDetails("some-name", "123/45678"), Left(DataNotFoundError))
        mockInternalError(InternalServerError)

        val result = underTest.submit(taxYearEOY, contractor = None).apply(fakeIndividualRequest.withFormUrlEncodedBody(
          ContractorDetailsForm.contractorName -> "some-name",
          ContractorDetailsForm.employerReferenceNumber -> "123/45678"
        ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "contractor provided" in {
        mockEndOfYearWithSessionData(taxYearEOY, aCisUserData.copy(employerRef = "123/45678"))
        mockSaveContractorDetails(taxYearEOY, aUser, Some(aCisUserData.copy(employerRef = "123/45678")), ContractorDetails("some-name", "123/45678"), Left(DataNotFoundError))
        mockInternalError(InternalServerError)

        val result = underTest.submit(taxYearEOY, contractor = Some("123/45678")).apply(fakeIndividualRequest.withFormUrlEncodedBody(
          ContractorDetailsForm.contractorName -> "some-name",
          ContractorDetailsForm.employerReferenceNumber -> "123/45678"
        ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to Deduction period on successful submission when" when {
      "no contractor provided" in {
        val newCisCYAModel = CisCYAModel(contractorName = Some("some-name"))
        mockNotInYear(taxYearEOY)
        mockSaveContractorDetails(taxYearEOY, aUser, None, ContractorDetails("some-name", "123/45678"), Right(aCisUserData.copy(employerRef = "123/45678", cis = newCisCYAModel)))

        await(underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withFormUrlEncodedBody(
          ContractorDetailsForm.contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> "123/45678"))) shouldBe
          Redirect(DeductionPeriodController.show(taxYearEOY, contractor = "123/45678")).addingToSession(SessionValues.TEMP_EMPLOYER_REF -> "123/45678")(fakeIndividualRequest)
      }

      "contractor provided" in {
        mockEndOfYearWithSessionData(taxYearEOY, aCisUserData.copy(employerRef = "123/45678"))
        val cisUserData = aCisUserData.copy(employerRef = "123/45678")
        mockSaveContractorDetails(taxYearEOY, aUser, Some(cisUserData), ContractorDetails("some-name", "123/45678"), Right(cisUserData))

        val month = cisUserData.cis.periodData.get.deductionPeriod.toString.toLowerCase
        await(underTest.submit(taxYear = taxYearEOY, contractor = Some("123/45678"))(fakeIndividualRequest.withFormUrlEncodedBody(
          ContractorDetailsForm.contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> "123/45678"))) shouldBe
          Redirect(ContractorCYAController.show(taxYearEOY, month, contractor = "123/45678"))
      }
    }
  }
}
