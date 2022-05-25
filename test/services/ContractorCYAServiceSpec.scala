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

package services

import audit.{AmendCisContractorAudit, CreateNewCisContractorAudit}
import config.MockAuditService
import models.mongo.DataNotUpdatedError
import models.{APIErrorBodyModel, APIErrorModel, HttpParserError, InvalidOrUnfinishedSubmission}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder._
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.submission.CISSubmissionBuilder.aCISSubmission
import support.builders.models.submission.PeriodDataBuilder.aPeriodData
import support.mocks.{MockCISConnector, MockCISSessionService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month._
import scala.concurrent.ExecutionContext.Implicits.global

class ContractorCYAServiceSpec extends UnitTest
  with MockCISSessionService
  with MockCISConnector
  with TaxYearProvider
  with MockAuditService {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> aUser.mtditid)

  private val updateCisSubmission = aCISSubmission.copy(
    employerRef = None,
    contractorName = None,
    periodData = Seq(aPeriodData, aPeriodData.copy(deductionFromDate = "2021-10-06", deductionToDate = "2021-11-05")),
    submissionId = Some("submissionId")
  )

  private val underTest = new ContractorCYAService(mockCISSessionService, mockAuditService, mockCISConnector)

  ".submitCisDeductionCYA" should {
    "return right when submit and clear are successful" in {
      val audit = AmendCisContractorAudit(taxYearEOY, employerRef = aCisUserData.employerRef, user = aUser, cisUserData = aCisUserData, incomeTaxUserData = anIncomeTaxUserData)

      mockSendAudit(audit.toAuditModel)
      mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData))
      mockSubmit(aUser.nino, taxYearEOY, updateCisSubmission, Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData)) shouldBe Right(())
    }

    "return error when clear fails" in {
      val audit = AmendCisContractorAudit(taxYearEOY, employerRef = aCisUserData.employerRef, user = aUser, cisUserData = aCisUserData, incomeTaxUserData = anIncomeTaxUserData)

      mockSendAudit(audit.toAuditModel)
      mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData))
      mockSubmit(aUser.nino, taxYearEOY, updateCisSubmission, Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Left(DataNotUpdatedError))

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData)) shouldBe Left(DataNotUpdatedError)
    }

    "send CreateNewCisContractorAudit event when a new contractor is created" in {
      val cisData = aCisUserData.copy(submissionId = None, cis = aCisCYAModel.copy(
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = NOVEMBER)),
        priorPeriodData = Seq()
      ))
      val audit = CreateNewCisContractorAudit.mapFrom(taxYearEOY, employerRef = cisData.employerRef, user = aUser, cisUserData = cisData).get

      mockSendAudit(audit.toAuditModel)
      mockSubmit(aUser.nino, taxYearEOY, aCISSubmission.copy(
        periodData = Seq(aPeriodData.copy(deductionFromDate = "2021-10-06", deductionToDate = "2021-11-05"))
      ), Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      await(underTest.submitCisDeductionCYA(taxYearEOY, cisData.employerRef, aUser, cisData)) shouldBe Right(())
    }

    "return error when submit fails" in {
      mockSubmit(aUser.nino, taxYearEOY, updateCisSubmission, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData)) shouldBe Left(HttpParserError(500))
    }

    "return InvalidOrUnfinishedSubmission error unable to submit" in {
      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData.copy(cis =
        aCisCYAModel.copy(contractorName = None),
        submissionId = None
      ))) shouldBe Left(InvalidOrUnfinishedSubmission)
    }

    "send AmenCisContractorAudit event and clear data when an existing deduction period data is amended" in {
      val cisData = aCisUserData.copy(cis = aCisCYAModel.copy(
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = NOVEMBER, grossAmountPaid = Some(350))),
        priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = JULY), aCYAPeriodData)
      ))
      val audit = AmendCisContractorAudit(taxYearEOY, employerRef = cisData.employerRef, user = aUser, cisUserData = cisData, anIncomeTaxUserData)

      mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData))
      mockSendAudit(audit.toAuditModel)
      mockSubmit(aUser.nino, taxYearEOY, cisData.toSubmission.get, Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      await(underTest.submitCisDeductionCYA(taxYearEOY, cisData.employerRef, aUser, cisData)) shouldBe Right(())
    }

    "not send AmenCisContractorAudit event when getPriorData result is an error" in {
      val cisData = aCisUserData.copy(cis = aCisCYAModel.copy(
        periodData = Some(aCYAPeriodData.copy(deductionPeriod = NOVEMBER, grossAmountPaid = Some(350))),
        priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = JULY), aCYAPeriodData)
      ))

      mockGetPriorData(taxYearEOY, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))
      mockSubmit(aUser.nino, taxYearEOY, cisData.toSubmission.get, Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      await(underTest.submitCisDeductionCYA(taxYearEOY, cisData.employerRef, aUser, cisData)) shouldBe Right(())
    }
  }
}
