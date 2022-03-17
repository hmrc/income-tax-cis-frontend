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
import models.{DeductionPeriodNotFoundError, HttpParserError}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentType, redirectLocation, status}
import support.ControllerUnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder.aUser
import support.builders.models.pages.ContractorSummaryPageBuilder.aContractorSummaryPage
import support.mocks.{MockAuthorisedAction, MockContractorSummaryService, MockErrorHandler}
import utils.InYearUtil
import views.html.ContractorSummaryView

class ContractorSummaryControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockContractorSummaryService
  with MockErrorHandler {

  private val pageView = inject[ContractorSummaryView]

  private val controller = new ContractorSummaryController(
    mockAuthorisedAction,
    mockContractorSummaryService,
    mockErrorHandler,
    new InYearUtil,
    pageView
  )

  "redirect to UnauthorisedUserErrorController when authentication fails" in {
    mockFailToAuthenticate()

    await(controller.show(taxYear, aCisDeductions.employerRef)(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
  }

  "redirect to overview page when the taxYear is not in year" in {
    mockAuthAsIndividual(Some(aUser.nino))

    val result = controller.show(taxYear = taxYearEOY, aCisDeductions.employerRef).apply(fakeIndividualRequest)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
  }

  "return INTERNAL_SERVER_ERROR when ContractorSummaryService returns HttpParserError" in {
    mockAuthAsIndividual(Some(aUser.nino))
    mockPageModelFor(taxYear = taxYear, aUser, employerRef = "some-ref", result = Left(HttpParserError(500)))
    mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

    await(controller.show(taxYear, contractor = "some-ref").apply(fakeIndividualRequest)) shouldBe InternalServerError
  }

  "redirect to the overview page when ContractorSummaryService returns an error that isn't HttpParserError" in {
    mockAuthAsIndividual(Some(aUser.nino))
    mockPageModelFor(taxYear = taxYear, aUser, employerRef = "some-ref", result = Left(DeductionPeriodNotFoundError))

    val result = controller.show(taxYear, contractor = "some-ref").apply(fakeIndividualRequest)

    status(result) shouldBe SEE_OTHER
    redirectLocation(result) shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
  }

  "return a successful response" in {
    mockAuthAsIndividual(Some(aUser.nino))
    mockPageModelFor(taxYear = taxYear, aUser, aCisDeductions.employerRef, result = Right(aContractorSummaryPage))

    val result = controller.show(taxYear, contractor = aCisDeductions.employerRef).apply(fakeIndividualRequest)

    status(result) shouldBe OK
    contentType(result) shouldBe Some("text/html")
  }
}
