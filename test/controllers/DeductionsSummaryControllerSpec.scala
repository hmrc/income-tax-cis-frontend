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
import models.{EmptyCisDataError, IncomeTaxUserDataHttpParserError}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentType, status}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.pages.DeductionsSummaryPageBuilder.aDeductionsSummaryPage
import support.mocks.{MockAuthorisedAction, MockDeductionsSummaryService, MockErrorHandler}
import views.html.DeductionsSummaryView

class DeductionsSummaryControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockDeductionsSummaryService
  with MockErrorHandler {

  private val pageView = inject[DeductionsSummaryView]

  private val underTest = new DeductionsSummaryController(
    mockAuthorisedAction,
    pageView,
    mockDeductionsSummaryService,
    mockErrorHandler
  )

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.show(taxYear = taxYearEOY)(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "return INTERNAL_SERVER_ERROR when deductionsSummaryService returns IncomeTaxUserDataHttpParserError" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYear, aUser, Left(IncomeTaxUserDataHttpParserError(500)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.show(taxYear).apply(fakeIndividualRequest)) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when deductionsSummaryService returns error different than IncomeTaxUserDataHttpParserError" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYear, aUser, Left(EmptyCisDataError))

      await(underTest.show(taxYear).apply(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYear, aUser, Right(aDeductionsSummaryPage))

      val result = underTest.show(taxYear).apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }
}
