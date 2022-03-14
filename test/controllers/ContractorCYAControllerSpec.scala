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
import models.{EmployerRefNotFoundError, HttpParserError}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.test.Helpers.{contentType, status}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.pages.ContractorCYAPageBuilder.aContractorCYAPage
import support.mocks.{MockAuthorisedAction, MockContractorCYAService, MockErrorHandler}
import views.html.ContractorCYAView

import java.time.Month

class ContractorCYAControllerSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockContractorCYAService
  with MockErrorHandler {

  private val pageView = inject[ContractorCYAView]

  private val underTest = new ContractorCYAController(
    mockAuthorisedAction,
    pageView,
    mockContractorCYAService,
    mockErrorHandler
  )

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.show(taxYear = taxYearEOY, Month.MAY.toString, contractor = "some-ref")(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "return INTERNAL_SERVER_ERROR when contractorCYAService returns HttpParserError" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYear, Month.MAY, refNumber = "some-ref", aUser, Left(HttpParserError(500)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      await(underTest.show(taxYear, Month.MAY.toString, contractor = "some-ref").apply(fakeIndividualRequest)) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when contractorCYAService returns error different than HttpParserError" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYear, Month.MAY, refNumber = "some-ref", aUser, result = Left(EmployerRefNotFoundError))

      await(underTest.show(taxYear, Month.MAY.toString, "some-ref").apply(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockPageModelFor(taxYear, Month.MAY, refNumber = "12345", aUser, result = Right(aContractorCYAPage))

      val result = underTest.show(taxYear, Month.MAY.toString, contractor = "12345").apply(fakeIndividualRequest)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("text/html")
    }
  }
}
