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

import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import controllers.errors.routes.UnauthorisedUserErrorController
import models.CisUserIsPriorSubmission
import models.mongo.{DataNotFoundError, DatabaseError}
import models.pages.ContractorDetailsViewModel
import play.api.http.Status.{BAD_REQUEST, SEE_OTHER}
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockActionsProvider, MockAuthorisedAction, MockContractorDetailsService, MockErrorHandler}
import utils.InYearUtil
import views.html.ContractorDetailsView

import scala.concurrent.Future

class ContractorDetailsControllerSpec extends ControllerUnitTest
  with MockActionsProvider
  with MockContractorDetailsService
  with MockErrorHandler {

  private val pageView = inject[ContractorDetailsView]

  val inYearAction = new InYearUtil

  private val underTest = new ContractorDetailsController()(cc, mockActionsProvider, pageView,
    mockContractorDetailsService, mockErrorHandler, ec, appConfig)

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.show(taxYear = taxYearEOY, None)(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))
      await(underTest.show(taxYear = taxYear, None)(fakeIndividualRequest)) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "redirect when service returns left" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckAccessContractorDetailsPage(taxYearEOY, aUser, Future(Left(CisUserIsPriorSubmission)))
      mockInternalError(InternalServerError)
      await(underTest.show(taxYear = taxYearEOY, Some("ERN"))(fakeIndividualRequest)) shouldBe InternalServerError

    }

    "Show view when service returns Right(None)" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckAccessContractorDetailsPage(taxYearEOY, aUser, Future(Right(None)))
      await(underTest.show(taxYear = taxYearEOY, Some("ERN")).apply(fakeIndividualRequest)).header.status shouldBe Ok.header.status
    }

    "Show view when service returns Right(Some)" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckAccessContractorDetailsPage(taxYearEOY, aUser, Future(Right(Some(aCisUserData))))
      await(underTest.show(taxYear = taxYearEOY, Some("ERN")).apply(fakeIndividualRequest)).header.status shouldBe Ok.header.status
    }
  }

  ".submit" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      await(underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))
      await(underTest.submit(taxYear = taxYear, contractor = None)(fakeIndividualRequest)) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return bad request when form is broken" in {
      mockAuthAsIndividual(Some(aUser.nino))
      await(underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withFormUrlEncodedBody("contractorName"-> "ABC Steelworks"))).header.status shouldBe
        BAD_REQUEST
    }

    "return ok when service returns left" in {
      val error = DataNotFoundError
      mockAuthAsIndividual(Some(aUser.nino))
      mockCreateOrUpdateContractorDetails(ContractorDetailsViewModel("ABC Steelworks", "123/AB12345"), taxYearEOY, aUser, Future(Left(error)))
      mockInternalError(InternalServerError)
      await(underTest.submit(taxYear = taxYearEOY, contractor = None)
      (fakeIndividualRequest.withFormUrlEncodedBody("contractorName"-> "ABC Steelworks",  "employerReferenceNumber" -> "123/AB12345"))) shouldBe InternalServerError

    }

    "return SEE_OTHER when service returns right" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockCreateOrUpdateContractorDetails(ContractorDetailsViewModel("ABC Steelworks", "123/AB12345"), taxYearEOY, aUser, Future(Right()))

      val result = await(underTest.submit(taxYear = taxYearEOY, contractor = None)(fakeIndividualRequest.withFormUrlEncodedBody("contractorName"-> "ABC Steelworks",  "employerReferenceNumber" -> "123/AB12345")))
      result.header.status shouldBe SEE_OTHER
    }
  }

}
