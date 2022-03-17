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
import controllers.errors.routes.UnauthorisedUserErrorController
import controllers.routes.LabourPayController
import forms.DeductionPeriodFormProvider
import models.HttpParserError
import models.mongo.{DataNotFoundError, DataNotUpdatedError}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentType, redirectLocation, status}
import support.ControllerUnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.pages.DeductionPeriodPageBuilder.aDeductionPeriodPage
import support.mocks.{MockAuthorisedAction, MockCISSessionService, MockDeductionPeriodService, MockErrorHandler}
import utils.InYearUtil
import utils.UrlUtils.encode
import views.html.cis.DeductionPeriodView

import java.time.Month

class DeductionPeriodControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockDeductionPeriodService
  with MockCISSessionService
  with MockErrorHandler {

  private val form = new DeductionPeriodFormProvider()

  private val pageView = inject[DeductionPeriodView]

  private val underTest = new DeductionPeriodController(
    mockAuthorisedAction,
    pageView,
    new InYearUtil,
    mockDeductionPeriodService,
    mockErrorHandler,
    form,
    mockCISSessionService,
    cc,
    ec,
    appConfig
  )

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.show(taxYear = taxYearEOY, contractor = aCisDeductions.employerRef)(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "return INTERNAL_SERVER_ERROR when DeductionPeriodService returns DataNotFound" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Left(DataNotFoundError))
      mockGetPriorAndMakeCYA(taxYearEOY, aCisDeductions.employerRef, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.show(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))) shouldBe InternalServerError
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))

      val result = underTest.show(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }

    "return redirect when no data in response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(None))

      val result = underTest.show(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/overview"
    }
    "return redirect when no months to submit for" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage.copy(priorSubmittedPeriods = Month.values()))))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage.copy(priorSubmittedPeriods = Month.values()))))

      val result = underTest.show(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/overview"
    }
    "return redirect when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val result = underTest.show(taxYear, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/overview"
    }
    "handle error response from getting data" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      val result = underTest.show(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  ".submit" should {
    "return redirect when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val result = underTest.submit(taxYear, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYear.toString).withFormUrlEncodedBody("month" -> "june"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/overview"
    }
    "return redirect when finds no data" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(None))

      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/overview"
    }
    "return INTERNAL_SERVER_ERROR when DeductionPeriodService returns DataNotFound" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      await(underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june"))) shouldBe InternalServerError
    }
    "submit the month period" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))
      mockSubmitMonth(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JUNE, Right(aCisUserData))

      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe LabourPayController.show(taxYearEOY, Month.JUNE.toString, encode(aCisDeductions.employerRef)).url
    }
    "redirect when no months can be added" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage.copy(priorSubmittedPeriods = Month.values()))))

      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe "/overview"
    }
    "handle submit failure" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))
      mockSubmitMonth(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JUNE, Left(DataNotUpdatedError))
      mockInternalError(InternalServerError)

      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "june"))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
    "return bad request when the month period is invalid" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aDeductionPeriodPage)))

      val result = underTest.submit(taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest
        .withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("month" -> "april"))

      status(result) shouldBe BAD_REQUEST
    }
  }
}
