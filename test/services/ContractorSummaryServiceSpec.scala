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

import models.mongo.{CisCYAModel, DataNotUpdatedError}
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder._
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockCISSessionService
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ContractorSummaryServiceSpec extends UnitTest
  with MockCISSessionService
  with TaxYearProvider {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new ContractorSummaryService(mockCISSessionService)

  val cya: CisCYAModel = aCisDeductions.toCYA(None, anIncomeTaxUserData.contractorPeriodsFor(aCisDeductions.employerRef))

  ".saveCYAForNewCisDeduction" should {
    "return right when save is successful" in {

      mockCreateOrUpdateCISUserData(taxYearEOY, aUser, aCisDeductions.employerRef, aCisDeductions.submissionId, isPriorSubmission = true, cya, Right(aCisUserData))

      await(underTest.saveCYAForNewCisDeduction(taxYearEOY, aCisDeductions.employerRef, anIncomeTaxUserData, aUser)) shouldBe Right(())
    }
    "return error when save fails" in {
      mockCreateOrUpdateCISUserData(taxYearEOY, aUser, aCisDeductions.employerRef, aCisDeductions.submissionId, isPriorSubmission = true, cya, Left(DataNotUpdatedError))

      await(underTest.saveCYAForNewCisDeduction(taxYearEOY, aCisDeductions.employerRef, anIncomeTaxUserData, aUser)) shouldBe Left(DataNotUpdatedError)
    }
  }
}
