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

import common.SessionValues
import controllers.errors.routes.UnauthorisedUserErrorController
import models.{HttpParserError, IncomeTaxUserData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockAuthorisedAction, MockCISSessionService, MockErrorHandler}
import utils.InYearUtil

import java.time.Month

class ActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockCISSessionService
  with MockErrorHandler {

  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")

  private val actionsProvider = new ActionsProvider(
    mockAuthorisedAction,
    mockCISSessionService,
    mockErrorHandler,
    new InYearUtil,
    appConfig
  )

  private val validTaxYears = validTaxYearList.mkString(",")

  ".priorCisDeductionsData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.priorCisDeductionsData(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "get prior data when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(IncomeTaxUserData(cis = Some(anAllCISDeductions))))

      val underTest = actionsProvider.priorCisDeductionsData(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.priorCisDeductionsData(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when authorised user with in year cis deductions" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(anAllCISDeductions))))

      val underTest = actionsProvider.priorCisDeductionsData(taxYear)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".inYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYear(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYear(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYear(taxYear)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".notInYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.notInYear(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYear(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.notInYear(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".inYearWithPreviousDataFor(taxYear, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".userPriorDataFor(taxYear, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.userPriorDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when in year" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(customerCISDeductions = None, contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.userPriorDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }

    "return successful response when end of year" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = None, customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY, contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".inYearWithPreviousDataFor(taxYear, month, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, month = "may", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, month = "may", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, month = "may", contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when no deductions for employer ref and month found" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref", periodData = Seq(aPeriodData.copy(deductionPeriod = Month.JUNE)))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, month = "may", contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref", periodData = Seq(aPeriodData.copy(deductionPeriod = Month.JUNE)))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, month = "june", contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".endOfYearWithSessionData(taxYear, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData)))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, contractor = "some/ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".endOfYearWithSessionData(taxYear, month, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, month = "May", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Error page when month is wrong" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockInternalError(InternalServerError)

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, month = "wrong-month", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, month = "May", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData)))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, month = "May", contractor = "some/ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString, SessionValues.VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }
}
