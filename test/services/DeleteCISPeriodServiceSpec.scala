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

import models.{APIErrorBodyModel, APIErrorModel, HttpParserError, InvalidOrUnfinishedSubmission}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CISSubmissionBuilder.anUpdateCISSubmission
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.{CISSubmissionBuilder, PeriodDataBuilder}
import support.mocks.{MockCISConnector, MockCISSessionService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Month
import scala.concurrent.ExecutionContext.Implicits.global

class DeleteCISPeriodServiceSpec extends UnitTest
  with MockCISSessionService
  with MockCISConnector
  with TaxYearProvider {

  val underTest = new DeleteCISPeriodService(mockCISSessionService,mockCISConnector)

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> aUser.mtditid)

  ".remove CISDeduction" should {
    "return invalid or unfinished submission" in {
      await(underTest.removeCisDeduction(taxYearEOY, employerRef = "employerRef", user = aUser, deductionPeriod = Month.MAY, anIncomeTaxUserData)) shouldBe Left(InvalidOrUnfinishedSubmission)
    }

    "return API error when submit fails" in {

      mockSubmit(aUser.nino, taxYearEOY, anUpdateCISSubmission.copy(
        periodData = Seq(CISSubmissionBuilder.aPeriodData.copy(
          grossAmountPaid = Some(450),
          deductionAmount = 100,
          costOfMaterials = Some(50)
        ))
      ), Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      val data = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(
          periodData = Seq(
            PeriodDataBuilder.aPeriodData,
            PeriodDataBuilder.aPeriodData.copy(
              deductionPeriod = Month.NOVEMBER
            )
          )
        ))))
      )))

      await(underTest.removeCisDeduction(taxYearEOY, employerRef = aCisDeductions.employerRef,
        user = aUser, deductionPeriod = Month.NOVEMBER, data)) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }
    "return Right(()) when submitted and refreshed data" in {

      mockSubmit(aUser.nino, taxYearEOY, anUpdateCISSubmission.copy(
        periodData = Seq(CISSubmissionBuilder.aPeriodData.copy(
          grossAmountPaid = Some(450),
          deductionAmount = 100,
          costOfMaterials = Some(50)
        ))
      ), Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      val data = anIncomeTaxUserData.copy(cis = Some(anAllCISDeductions.copy(
        contractorCISDeductions = None,
        customerCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(
          periodData = Seq(
            PeriodDataBuilder.aPeriodData,
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
