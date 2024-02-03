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

import common.SessionValues.TEMP_EMPLOYER_REF
import controllers.routes.{ContractorCYAController, DeductionPeriodController}
import forms.ContractorDetailsForm
import forms.ContractorDetailsForm.{contractorName, employerReferenceNumber}
import models.HttpParserError
import models.forms.ContractorDetails
import models.mongo.{CisCYAModel, DataNotFoundError}
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json.{stringify, toJson}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentAsString, contentType, status}
import sttp.model.Method.POST
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockCISSessionService, MockContractorDetailsService, MockErrorHandler}
import views.html.ContractorDetailsView

class ContractorDetailsControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockContractorDetailsService
  with MockErrorHandler
  with MockCISSessionService {

  private val pageView = inject[ContractorDetailsView]

  private val underTest = new ContractorDetailsController(
    mockActionsProvider,
    pageView,
    mockContractorDetailsService,
    mockErrorHandler,
    mockCISSessionService
  )(cc, ec, appConfig)

  ".show" should {
    "return successful response when contractor not provided" in {
      mockNotInYear(taxYearEOY)
      mockGetPriorEmployerRefs(Right(Seq.empty))

      val result = underTest.show(taxYearEOY, None).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById(contractorName).attr("value") shouldBe ""
      document.getElementById(employerReferenceNumber).attr("value") shouldBe ""
    }

    "return error response when failed to get employerRefs" in {
      mockNotInYear(taxYearEOY)
      mockGetPriorEmployerRefs(Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val result = underTest.show(taxYearEOY, None).apply(fakeIndividualRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return successful response when contractor provided" in {
      mockNotInYear(taxYearEOY)
      mockGetPriorEmployerRefs(Right(Seq.empty))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "contractor-ref", Right(Some(aCisUserData.copy(employerRef = "contractor-ref", isPriorSubmission = false))))

      val result = underTest.show(taxYearEOY, Some("contractor-ref")).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById(contractorName).attr("value") shouldBe aCisUserData.cis.contractorName.get
      document.getElementById(employerReferenceNumber).attr("value") shouldBe "contractor-ref"
    }

    "return error response when contractor provided but fails to get employerRefs" in {
      mockNotInYear(taxYearEOY)
      mockGetPriorEmployerRefs(Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val result = underTest.show(taxYearEOY, Some("contractor-ref")).apply(fakeIndividualRequest)

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "display previous values even when identifier missing from query string (happens when back button used)" in {
      mockNotInYear(taxYearEOY)
      mockGetPriorEmployerRefs(Right(Seq.empty))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "contractor-ref", Right(Some(aCisUserData.copy(employerRef = "contractor-ref", isPriorSubmission = false))))

      val result = underTest.show(taxYearEOY, None).apply(
        fakeIndividualRequest.withSession(TEMP_EMPLOYER_REF -> "contractor-ref"))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.getElementById(contractorName).attr("value") shouldBe aCisUserData.cis.contractorName.get
      document.getElementById(employerReferenceNumber).attr("value") shouldBe "contractor-ref"
    }
  }

  ".submit" should {
    "return page with error when validation fails" when {
      "employer ref not supplied in query string" in {
        mockNotInYear(taxYearEOY)
        mockGetPriorEmployerRefs(Right(Seq.empty))

        val result = underTest.submit(taxYear = taxYearEOY, contractor = None)(
          fakeIndividualRequest
            .withMethod(POST.method)
            .withFormUrlEncodedBody(contractorName -> "some-name", employerReferenceNumber -> "not a valid employer ref")
        )

        status(result) shouldBe BAD_REQUEST
        contentType(result) shouldBe Some("text/html")
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary").isEmpty shouldBe false
      }

      "employer ref supplied in query string and new employer ref is malformed" in {
        mockNotInYear(taxYearEOY)
        mockGetPriorEmployerRefs(Right(Seq.empty))

        val result = underTest.submit(taxYear = taxYearEOY, contractor = Some("123/45678"))(
          fakeIndividualRequest
            .withMethod(POST.method)
            .withSession(TEMP_EMPLOYER_REF -> "contractor-ref")
            .withFormUrlEncodedBody(contractorName -> "some-name", employerReferenceNumber -> "not a valid employer ref")
        )

        status(result) shouldBe BAD_REQUEST
        contentType(result) shouldBe Some("text/html")
        val document = Jsoup.parse(contentAsString(result))
        document.select(".govuk-error-summary").isEmpty shouldBe false
      }
    }

    "return error page when getting employerRefs fails" when {
      "no contractor provided" in {
        mockNotInYear(taxYearEOY)
        mockGetPriorEmployerRefs(Left(HttpParserError(INTERNAL_SERVER_ERROR)))
        mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

        val result = underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(contractorName -> "some-name"))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "contractor provided" in {
        mockNotInYear(taxYearEOY)
        mockGetPriorEmployerRefs(Left(HttpParserError(INTERNAL_SERVER_ERROR)))
        mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

        val result = underTest.submit(taxYearEOY, contractor = Some("123/45678")).apply(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
          contractorName -> "some-name",
          ContractorDetailsForm.employerReferenceNumber -> "123/45678"
        ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "handle internal server error when save operation fails with database error when" when {
      "no contractor provided" in {
        mockNotInYear(taxYearEOY)
        mockGetSessionData(taxYearEOY, aUser, employerRef = "123/45678", Right(Some(aCisUserData.copy(employerRef = "123/45678"))))
        mockGetPriorEmployerRefs(Right(Seq.empty))
        mockSaveContractorDetails(taxYearEOY, aUser, Some(aCisUserData.copy(employerRef = "123/45678")), ContractorDetails("some-name", "123/45678"), Left(DataNotFoundError))
        mockInternalServerError(InternalServerError)

        val result = underTest.submit(taxYearEOY, contractor = None).apply(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
          contractorName -> "some-name",
          ContractorDetailsForm.employerReferenceNumber -> "123/45678"
        ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }

      "contractor provided" in {
        mockNotInYear(taxYearEOY)
        mockGetSessionData(taxYearEOY, aUser, employerRef = "123/45678", Right(Some(aCisUserData.copy(employerRef = "123/45678"))))
        mockGetPriorEmployerRefs(Right(Seq.empty))
        mockSaveContractorDetails(taxYearEOY, aUser, Some(aCisUserData.copy(employerRef = "123/45678")), ContractorDetails("some-name", "123/45678"), Left(DataNotFoundError))
        mockInternalServerError(InternalServerError)

        val result = underTest.submit(taxYearEOY, contractor = Some("123/45678")).apply(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
          contractorName -> "some-name",
          ContractorDetailsForm.employerReferenceNumber -> "123/45678"
        ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to Deduction period on successful submission when" when {
      "no contractor provided" in {
        val newCisCYAModel = CisCYAModel(contractorName = Some("some-name"))
        mockNotInYear(taxYearEOY)
        mockGetSessionData(taxYearEOY, aUser, employerRef = "123/45678", Right(Some(aCisUserData.copy(employerRef = "123/45678"))))
        mockGetPriorEmployerRefs(Right(Seq.empty))
        mockSaveContractorDetails(taxYearEOY, aUser, Some(aCisUserData.copy(employerRef = "123/45678")), ContractorDetails("some-name", "123/45678"),
          Right(aCisUserData.copy(employerRef = "123/45678", cis = newCisCYAModel)))

        await(underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
          contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> "123/45678"))) shouldBe
          Redirect(DeductionPeriodController.show(taxYearEOY, contractor = "123/45678"))
            .addingToSession(TEMP_EMPLOYER_REF -> "123/45678")(fakeIndividualRequest)
      }

      "no contractor provided and using employer ref with spaces" in {
        val newCisCYAModel = CisCYAModel(contractorName = Some("some-name"))
        mockNotInYear(taxYearEOY)
        mockGetSessionData(taxYearEOY, aUser, employerRef = "123/45678", Right(Some(aCisUserData.copy(employerRef = "123/45678"))))
        mockGetPriorEmployerRefs(Right(Seq.empty))
        mockSaveContractorDetails(taxYearEOY, aUser, Some(aCisUserData.copy(employerRef = "123/45678")), ContractorDetails("some-name", "123/45678"),
          Right(aCisUserData.copy(employerRef = "123/45678", cis = newCisCYAModel)))

        await(underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
          contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> " 1 2 3 / 4 5 6 7 8"))) shouldBe
          Redirect(DeductionPeriodController.show(taxYearEOY, contractor = "123/45678"))
            .addingToSession(TEMP_EMPLOYER_REF -> "123/45678")(fakeIndividualRequest)
      }

      "contractor provided" in {
        mockNotInYear(taxYearEOY)
        mockGetSessionData(taxYearEOY, aUser, employerRef = "123/45678", Right(Some(aCisUserData.copy(employerRef = "123/45678"))))
        mockGetPriorEmployerRefs(Right(Seq.empty))
        val cisUserData = aCisUserData.copy(employerRef = "123/45678")
        mockSaveContractorDetails(taxYearEOY, aUser, Some(cisUserData), ContractorDetails("some-name", "123/45678"), Right(cisUserData))

        val month = cisUserData.cis.periodData.get.deductionPeriod.toString.toLowerCase
        await(underTest.submit(taxYear = taxYearEOY, contractor = Some("123/45678"))(fakeIndividualRequest.withMethod(POST.method).withFormUrlEncodedBody(
          contractorName -> "some-name", ContractorDetailsForm.employerReferenceNumber -> "123/45678"))) shouldBe
          Redirect(ContractorCYAController.show(taxYearEOY, month, contractor = "123/45678"))
      }
    }
  }
}
