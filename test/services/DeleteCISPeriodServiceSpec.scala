/*
 * Copyright 2023 HM Revenue & Customs
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

import audit.DeleteCisPeriodAudit
import config.{MockAuditService, MockNrsService}
import models.nrs.{ContractorDetails, DeleteCisPeriodPayload}
import models.{APIErrorBodyModel, APIErrorModel, HttpParserError, InvalidOrUnfinishedSubmission}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.{PeriodDataBuilder, submission}
import support.builders.models.submission.CISSubmissionBuilder.aCISSubmission
import support.mocks.{MockCISConnector, MockCISSessionService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month
import java.time.Month.NOVEMBER
import scala.concurrent.ExecutionContext.Implicits.global

class DeleteCISPeriodServiceSpec extends UnitTest
  with MockCISSessionService
  with MockCISConnector
  with MockAuditService
  with MockNrsService
  with TaxYearProvider {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> aUser.mtditid)

  private val updateCisSubmission = aCISSubmission.copy(
    employerRef = None,
    contractorName = None,
    periodData = Seq(submission.PeriodDataBuilder.aPeriodData.copy(
      grossAmountPaid = Some(450),
      deductionAmount = 100,
      costOfMaterials = Some(50)
    )),
    submissionId = Some("submissionId")
  )

  private val underTest = new DeleteCISPeriodService(mockCISSessionService, mockAuditService, mockNrsService, mockCISConnector)

  ".remove CISDeduction" should {
    "return invalid or unfinished submission when employerRef can't be found" in {
      await(underTest.removeCisDeduction(taxYearEOY, employerRef = "employerRef", user = aUser, deductionPeriod = Month.MAY, anIncomeTaxUserData)) shouldBe Left(InvalidOrUnfinishedSubmission)

    }
    "return invalid or unfinished submission when month can't be found" in {

      val data = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(periodData = Seq(PeriodDataBuilder.aPeriodData)))))
      )))

      await(underTest.removeCisDeduction(taxYearEOY, employerRef = aCisDeductions.employerRef, user = aUser, deductionPeriod = Month.NOVEMBER, data)) shouldBe Left(InvalidOrUnfinishedSubmission)
    }
    "return invalid or unfinished submission when no data" in {

      val emptyData = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = None
      )))
      await(underTest.removeCisDeduction(taxYearEOY, employerRef = aCisDeductions.employerRef, user = aUser, deductionPeriod = Month.NOVEMBER, emptyData)) shouldBe Left(InvalidOrUnfinishedSubmission)
    }
    "return API error when submit fails" in {
      mockSubmit(aUser.nino, taxYearEOY, updateCisSubmission, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val data = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(
          periodData = Seq(
            PeriodDataBuilder.aPeriodData,
            PeriodDataBuilder.aPeriodData.copy(
              deductionPeriod = NOVEMBER
            )
          )
        ))))
      )))

      await(underTest.removeCisDeduction(taxYearEOY, employerRef = aCisDeductions.employerRef,
        user = aUser, deductionPeriod = NOVEMBER, data)) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }

    "return Right(()) when submitted and refreshed data" in {
      val periodData = aPeriodData.copy(deductionPeriod = NOVEMBER)
      val audit = DeleteCisPeriodAudit(taxYearEOY, aUser, aCisDeductions.contractorName, aCisDeductions.employerRef, periodData)
      val nrs = DeleteCisPeriodPayload(ContractorDetails(aCisDeductions.contractorName, aCisDeductions.employerRef), periodData)

      mockSendAudit(audit.toAuditModel)
      mockSendNrs(nrs)
      mockSubmit(aUser.nino, taxYearEOY, updateCisSubmission, Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      val data = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(
          periodData = Seq(aPeriodData, aPeriodData.copy(
              deductionPeriod = NOVEMBER
            )
          )
        ))))
      )))

      await(underTest.removeCisDeduction(taxYearEOY, employerRef = aCisDeductions.employerRef, user = aUser, deductionPeriod = NOVEMBER, data)) shouldBe Right(())
    }
    "return Right(()) when submitted and refreshed data when it's the last period" in {

      mockDelete(aUser.nino, taxYearEOY, aCisUserData.submissionId.get, Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))
      val periodData = aPeriodData.copy(deductionPeriod = NOVEMBER)
      val audit = DeleteCisPeriodAudit(taxYearEOY, aUser, aCisDeductions.contractorName, aCisDeductions.employerRef, periodData)
      val nrs = DeleteCisPeriodPayload(ContractorDetails(aCisDeductions.contractorName, aCisDeductions.employerRef), periodData)

      mockSendAudit(audit.toAuditModel)
      mockSendNrs(nrs)

      val data = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(
          periodData = Seq(
            PeriodDataBuilder.aPeriodData.copy(
              deductionPeriod = Month.NOVEMBER
            )
          )
        ))))
      )))

      await(underTest.removeCisDeduction(taxYearEOY, employerRef = aCisDeductions.employerRef, user = aUser, deductionPeriod = Month.NOVEMBER, data)) shouldBe Right(())
    }
  }
}
