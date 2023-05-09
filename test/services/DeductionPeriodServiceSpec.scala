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

import models.mongo.{CYAPeriodData, DataNotFoundError, DataNotUpdatedError}
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockCISSessionService
import support.{FakeRequestHelper, TaxYearProvider, UnitTest}

import java.time.Month
import scala.concurrent.ExecutionContext.Implicits.global

class DeductionPeriodServiceSpec extends UnitTest
  with MockCISSessionService
  with TaxYearProvider
  with FakeRequestHelper {

  private val underTest = new DeductionPeriodService(mockCISSessionService)

  ".submitDeductionPeriod" should {
    "submit and save the data" in {
      val default = CYAPeriodData(deductionPeriod = Month.JANUARY, contractorSubmitted = false, originallySubmittedPeriod = None)
      val cya = aCisUserData.cis
      val periodData = cya.periodData.map(_.copy(deductionPeriod = Month.JANUARY)).getOrElse(default)
      val updatedCYA = cya.copy(periodData = Some(periodData))

      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(Some(aCisUserData)))
      mockCreateOrUpdateCISUserData(taxYearEOY, aUser, aCisDeductions.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Right(aCisUserData))

      await(underTest.submitDeductionPeriod(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JANUARY)) shouldBe Right(aCisUserData)
    }

    "handle not getting data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(None))

      await(underTest.submitDeductionPeriod(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JANUARY)) shouldBe Left(DataNotFoundError)
    }

    "handle not saving the data" in {
      val default = CYAPeriodData(deductionPeriod = Month.JANUARY, contractorSubmitted = false, originallySubmittedPeriod = None)
      val cya = aCisUserData.cis
      val periodData = cya.periodData.map(_.copy(deductionPeriod = Month.JANUARY)).getOrElse(default)
      val updatedCYA = cya.copy(periodData = Some(periodData))

      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(Some(aCisUserData)))
      mockCreateOrUpdateCISUserData(taxYearEOY, aUser, aCisDeductions.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, updatedCYA, Left(DataNotUpdatedError))

      await(underTest.submitDeductionPeriod(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JANUARY)) shouldBe Left(DataNotUpdatedError)
    }
  }
}
