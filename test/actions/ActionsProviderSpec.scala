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

import common.SessionValues._
import config.{AppConfig, MockAuditService, MockNrsService}
import controllers.errors.routes.UnauthorisedUserErrorController
import controllers.routes.{ContractorDetailsController, DeductionPeriodController}
import models.{HttpParserError, IncomeTaxUserData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{InternalServerError, Ok, Redirect}
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers.status
import support.ControllerUnitTest
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.ContractorDetailsAndPeriodDataBuilder.aContractorDetailsAndPeriodData
import support.builders.models.audit.ViewCisPeriodAuditBuilder.aViewCisPeriodAudit
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.nrs.DeductionPeriodBuilder.aDeductionPeriod
import support.builders.models.nrs.ViewCisPeriodPayloadBuilder.aViewCisPeriodPayload
import support.mocks.{MockAppConfig, MockAuthorisedAction, MockCISSessionService, MockErrorHandler}
import utils.InYearUtil

import java.time.Month

class ActionsProviderSpec extends ControllerUnitTest
  with MockAuthorisedAction
  with MockCISSessionService
  with MockAuditService
  with MockNrsService
  with MockErrorHandler {

  private val anyBlock = (_: Request[AnyContent]) => Ok("any-result")

  private val actionsProvider = createActionsProvider(appConfig)

  private val validTaxYears = validTaxYearList.mkString(",")

  private def createActionsProvider(appConfig: AppConfig) = new ActionsProvider(
    mockAuthorisedAction,
    mockCISSessionService,
    mockAuditService,
    mockNrsService,
    mockErrorHandler,
    new InYearUtil,
    appConfig
  )

  ".priorCisDeductionsData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.priorCisDeductionsData(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.priorCisDeductionsData(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "get prior data when end of year" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(contractorCISDeductions = None)))))

      val underTest = actionsProvider.priorCisDeductionsData(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }

    "return successful response in year with in year cis deductions" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(anAllCISDeductions))))

      val underTest = actionsProvider.priorCisDeductionsData(taxYear)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".notInYear" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYear(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYear(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYear(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".tailoringEnabledFilter" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.tailoringEnabledFilter(taxYearEOY)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when tailoring is disabled" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.tailoringEnabledFilter(taxYear)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response when tailoring is enabled" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = createActionsProvider(new MockAppConfig().config(enableTailoring = true)).endOfYear(taxYearEOY)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".inYearWithPreviousDataFor(taxYear, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
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

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when in year" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(customerCISDeductions = None, contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.userPriorDataFor(taxYear, contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }

    "return successful response when end of year" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = None, customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY, contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".userPriorDataFor(taxYear, contractor, month)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY, contractor = "any-contractor", "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString,
        VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.userPriorDataFor(taxYear, contractor = "some-ref", month = "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString,
        VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when in year" in {
      val allCISDeductions = anAllCISDeductions.copy(customerCISDeductions = None)

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))
      mockSendAudit(aViewCisPeriodAudit.toAuditModel)
      mockSendNrs(aViewCisPeriodPayload)

      val underTest = actionsProvider.userPriorDataFor(taxYear, contractor = aCisDeductions.employerRef, month = "may")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }

    "return successful response when end of year" in {
      val viewAudit = aViewCisPeriodAudit.copy(taxYear = taxYearEOY)

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(IncomeTaxUserData(cis = Some(anAllCISDeductions))))
      mockSendAudit(viewAudit.toAuditModel)
      mockSendNrs(aViewCisPeriodPayload)

      val underTest = actionsProvider.userPriorDataFor(taxYearEOY, contractor = aCisDeductions.employerRef, month = "may")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".exclusivelyCustomerPriorDataForEOY(taxYear, contractor, month)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.exclusivelyCustomerPriorDataForEOY(taxYearEOY, contractor = "any-contractor", month = "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest)) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.exclusivelyCustomerPriorDataForEOY(taxYear, contractor = "any-contractor", month = "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.exclusivelyCustomerPriorDataForEOY(taxYearEOY, contractor = "some-ref", month = "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "return successful response when end of year" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref")
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = None, customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYearEOY, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.exclusivelyCustomerPriorDataForEOY(taxYearEOY, contractor = "some-ref", month = "may")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".inYearWithPreviousDataFor(taxYear, month, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, month = "may", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when not in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYearEOY, month = "may", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "handle internal server error when getPriorData result in error" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, month = "may", contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when no deductions for employer ref and month found" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref", periodData = Seq(aPeriodData.copy(deductionPeriod = Month.JUNE)))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, month = "may", contractor = "some-ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      val deductions = aCisDeductions.copy(employerRef = "some-ref", periodData = Seq(aPeriodData.copy(deductionPeriod = Month.JUNE)))
      val allCISDeductions = anAllCISDeductions.copy(contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(deductions))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockGetPriorData(taxYear, aUser, Right(IncomeTaxUserData(cis = Some(allCISDeductions))))

      val underTest = actionsProvider.inYearWithPreviousDataFor(taxYear, month = "june", contractor = "some-ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".endOfYearWithSessionData(taxYear, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, contractor = "any-contractor", redirectIfPrior = false)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, contractor = "any-contractor", redirectIfPrior = false)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "redirect to Income Tax Submission Overview when redirectIfPrior is true and it's a prior submission" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData)))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, contractor = "some/ref", redirectIfPrior = true)(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData)))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, contractor = "some/ref", redirectIfPrior = false)(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".endOfYearWithSessionData(taxYear, month, contractor)" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, month = "May", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Error page when month is wrong" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockInternalServerError(InternalServerError)

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, month = "wrong-month", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYear, month = "May", contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "return successful response" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData)))

      val underTest = actionsProvider.endOfYearWithSessionData(taxYearEOY, month = "May", contractor = "some/ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear, contractor)" should {
    "redirect to Error page when month is wrong" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockInternalServerError(InternalServerError)

      val underTest = actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear, month = Some("wrong-month"), contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe InternalServerError
    }

    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when in year" in {
      mockAuthAsIndividual(Some(aUser.nino))

      val underTest = actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYear, contractor = "any-contractor")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYear.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    "redirect to Income Tax Submission Overview when session data is None" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(None))

      val underTest = actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, contractor = "some/ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "redirect to Income Tax Submission Overview when contractorSubmitted is true" in {
      val periodData = aCYAPeriodData.copy(contractorSubmitted = true)
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(periodData))))))

      val underTest = actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, contractor = "some/ref")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "return successful response" in {
      val periodData = aCYAPeriodData.copy(contractorSubmitted = false)
      mockAuthAsIndividual(Some(aUser.nino))
      mockGetSessionData(taxYearEOY, aUser, employerRef = "some/ref", result = Right(Some(aCisUserData.copy(cis = aCisCYAModel.copy(periodData = Some(periodData))))))

      val underTest = actionsProvider.endOfYearWithSessionDataWithCustomerDeductionPeriod(taxYearEOY, contractor = "some/ref")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }

  ".checkCyaExistsAndReturnSessionData" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()

      val underTest = actionsProvider.checkCyaExistsAndReturnSessionData(taxYearEOY, aCisDeductions.employerRef, "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString,
        VALID_TAX_YEARS -> validTaxYears))) shouldBe Redirect(UnauthorisedUserErrorController.show())
    }

    "redirect to Income Tax Submission Overview when Session data is None" in {
      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckCyaAndReturnData(taxYearEOY, employerRef = aCisDeductions.employerRef, Month.MAY, result = Right(None))

      val underTest = actionsProvider.checkCyaExistsAndReturnSessionData(taxYearEOY, aCisDeductions.employerRef, "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY))
    }

    "send ViewCisPeriodAudit when GET request and end of year" in {
      val contractorDetailsAndPeriodData = aContractorDetailsAndPeriodData.copy(labour = Some(500), cisDeduction = None, materialsCost = Some(250))
      val cisCYAModel = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(deductionAmount = None)))

      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckCyaAndReturnData(taxYearEOY, employerRef = aCisDeductions.employerRef, Month.MAY, result = Right(Some(aCisUserData.copy(cis = cisCYAModel))))
      mockSendAudit(aViewCisPeriodAudit.copy(taxYear = taxYearEOY, cisPeriod = contractorDetailsAndPeriodData).toAuditModel)

      val underTest = actionsProvider.checkCyaExistsAndReturnSessionData(taxYearEOY, aCisDeductions.employerRef, "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(DeductionPeriodController.show(taxYearEOY, aCisDeductions.employerRef))
    }

    "return a redirect to Contractor details url when it is not PriorSubmission and cisDeduction were not provided" in {
      val viewAudit = aViewCisPeriodAudit.copy(taxYear = taxYearEOY,
        cisPeriod = aContractorDetailsAndPeriodData.copy(labour = Some(500), materialsCost = Some(250), cisDeduction = None))
      val aCisUserDataNoPriorSubmission = aCisUserData.copy(isPriorSubmission = false)
        .copy(cis = aCisCYAModel.copy(periodData = Some(aCYAPeriodData.copy(deductionAmount = None))))

      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckCyaAndReturnData(taxYearEOY, employerRef = aCisDeductions.employerRef, Month.MAY, result = Right(Some(aCisUserDataNoPriorSubmission)))
      mockSendAudit(viewAudit.toAuditModel)

      val underTest = actionsProvider.checkCyaExistsAndReturnSessionData(taxYearEOY, aCisUserDataNoPriorSubmission.employerRef, "may")(block = anyBlock)

      await(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe
        Redirect(ContractorDetailsController.show(aCisUserDataNoPriorSubmission.taxYear, Some(aCisUserDataNoPriorSubmission.employerRef)).url)
    }

    "get session data" in {
      val cisPeriod = aContractorDetailsAndPeriodData.copy(labour = Some(500), materialsCost = Some(250))
      val viewAudit = aViewCisPeriodAudit.copy(taxYear = taxYearEOY, cisPeriod = cisPeriod)
      val viewNrsPayload = aViewCisPeriodPayload.
        copy(customerDeductionPeriod = aDeductionPeriod.copy(labour = Some(500), materialsCost = Some(250)))

      mockAuthAsIndividual(Some(aUser.nino))
      mockCheckCyaAndReturnData(taxYearEOY, employerRef = aCisDeductions.employerRef, Month.MAY, result = Right(Some(aCisUserData)))
      mockSendAudit(viewAudit.toAuditModel)
      mockSendNrs(viewNrsPayload)

      val underTest = actionsProvider.checkCyaExistsAndReturnSessionData(taxYearEOY, aCisDeductions.employerRef, "may")(block = anyBlock)

      status(underTest(fakeIndividualRequest.withSession(TAX_YEAR -> taxYearEOY.toString, VALID_TAX_YEARS -> validTaxYears))) shouldBe OK
    }
  }
}
