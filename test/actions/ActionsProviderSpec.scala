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

package actions

import config.MockAppConfig
import controllers.errors.routes.UnauthorisedUserErrorController
import controllers.routes.DeductionPeriodController
import models.mongo.DataNotFoundError
import play.api.http.Status.OK
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockAuthorisedAction, MockCISSessionService, MockErrorHandler}
import utils.{InYearUtil, UrlUtils}

class ActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockCISSessionService
  with MockErrorHandler {

  private val mockAppConfig = new MockAppConfig().config()
  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")

  private val actionsProvider = new ActionsProvider(
    mockAuthorisedAction,
    mockCISSessionService,
    mockErrorHandler,
    new InYearUtil,
    mockAppConfig
  )

  ".inYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYear(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYear(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYear(taxYear)(block = anyBlock)

      status(underTest(fakeIndividualRequest)) shouldBe OK
    }
  }

  ".notInYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.notInYear(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYear(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYear(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest)) shouldBe OK
    }
  }

  ".notInYearWithSessionData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYearWithSessionData(taxYear, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getting session data result in database error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Left(DataNotFoundError))
      mockInternalError(InternalServerError)

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when session data is None" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some-ref", Right(None))

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "redirect to Deduction Period Page when session data has no PeriodData" in {
      val cisUserDataWithoutPeriodData = aCisUserData.copy(cis = aCisCYAModel.copy(periodData = None), employerRef = "some/ref", taxYear = taxYearEOY)

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", Right(Some(cisUserDataWithoutPeriodData)))

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, contractor = UrlUtils.encode(value = "some/ref"))(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(DeductionPeriodController.show(taxYearEOY, UrlUtils.encode(value = "some/ref")))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData)))

      val underTest = actionsProvider.notInYearWithSessionData(taxYearEOY, contractor = UrlUtils.encode(value = "some/ref"))(block = anyBlock)

      status(underTest(fakeIndividualRequest)) shouldBe OK
    }
  }
}
