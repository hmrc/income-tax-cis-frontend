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

import controllers.errors.routes.UnauthorisedUserErrorController
import controllers.routes.DeductionPeriodController
import forms.FormsProvider
import models.mongo.DataNotFoundError
import models.{NoCYAPeriodDataError, NoCisUserDataError}
import org.jsoup.Jsoup
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentAsString, contentType, status}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockCISSessionService, MockErrorHandler, MockLabourPayService}
import views.html.LabourPayView

import java.time.Month

class LabourPayControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockLabourPayService
  with MockCISSessionService
  with MockErrorHandler {

  private val wrongAmountFormat = "amount" -> "2.3.4"
  private val pageView = inject[LabourPayView]
  private val formsProvider = new FormsProvider()

  private val underTest = new LabourPayController(
    mockActionsProvider,
    formsProvider,
    pageView,
    mockLabourPayService,
    mockErrorHandler
  )

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.show(taxYear = taxYearEOY, Month.MAY.toString, contractor = "some-ref")(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      await(underTest.show(taxYear = taxYear, Month.MAY.toString, contractor = "some-ref")(fakeIndividualRequest)) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getting session data result in database error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      await(underTest.show(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when session data is None" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(None))

      await(underTest.show(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", result = Right(Some(aCisUserData)))

      val result = underTest.show(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }

  ".submit" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.submit(taxYear = taxYearEOY, Month.MAY.toString, contractor = "some-ref")(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      await(underTest.submit(taxYear = taxYear, Month.MAY.toString, contractor = "some-ref")(fakeIndividualRequest)) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getting session data results in database error and form has error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      await(underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody(wrongAmountFormat))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when session data is None and form has error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(None))

      await(underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody(wrongAmountFormat))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "render page with error when validation of form fails" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))

      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody(wrongAmountFormat))

      status(result) shouldBe BAD_REQUEST
      contentType(result) shouldBe Some("text/html")
      val document = Jsoup.parse(contentAsString(result))
      document.select("#error-summary-title").isEmpty shouldBe false
    }

    "redirect to DeductionPeriod page when save operation fails with NoCYAPeriodDataError" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))
      mockSaveLabourPay(taxYearEOY, employerRef = "some-ref", aUser, amount = 123, result = Left(NoCYAPeriodDataError))

      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "123"))

      await(result) shouldBe Redirect(DeductionPeriodController.show(taxYearEOY, "some-ref"))
    }

    "handle internal server error when save operation fails with database error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))
      mockSaveLabourPay(taxYearEOY, employerRef = "some-ref", aUser, amount = 123, result = Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      val result = underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "123"))

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "redirect to Income Tax Submission Overview when service returns error different than DatabaseError" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))
      mockSaveLabourPay(taxYearEOY, employerRef = "some-ref", aUser, amount = 123, result = Left(NoCisUserDataError))

      await(underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "123"))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    // TODO: The following needs to change once we have a page to redirect to
    "redirect to Income Tax Submission Overview on successful submission" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(Some(aCisUserData)))
      mockSaveLabourPay(taxYearEOY, employerRef = "some-ref", aUser, amount = 123, result = Right(()))

      await(underTest.submit(taxYearEOY, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest.withFormUrlEncodedBody("amount" -> "123"))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }
  }
}
