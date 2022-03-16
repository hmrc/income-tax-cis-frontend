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

import java.time.Month

import models.mongo.{DataNotFound, DataNotUpdated}
import models.pages.DeductionPeriodPage
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.MockCISSessionService
import support.{TaxYearHelper, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class DeductionPeriodServiceSpec extends UnitTest
  with MockCISSessionService
  with TaxYearHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new DeductionPeriodService(
    mockCISSessionService
  )

  ".pageModelFor" should {

    "return error when fails to get data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Left(DataNotFound))

      await(underTest.pageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Left(DataNotFound)
    }

    "return none when no data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(None))

      await(underTest.pageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Right(None)
    }

    "return data when user has cya data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(Some(aCisUserData)))

      await(underTest.pageModelFor(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Right(Some(DeductionPeriodPage(
        taxYear = taxYearEOY,
        contractorName = Some("ABC Steelworks"),
        employerRef = "123/AB123456",
        period = Some(Month.MAY),
        priorSubmittedPeriods = Seq(Month.NOVEMBER)
      )))
    }
  }

  ".submitDeductionPeriod" should {
    "submit and save the data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(Some(aCisUserData)))
      mockCreateOrUpdateCISUserData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(aCisUserData))

      await(underTest.submitDeductionPeriod(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JANUARY)) shouldBe Right(aCisUserData)
    }
    "handle not getting data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(None))

      await(underTest.submitDeductionPeriod(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JANUARY)) shouldBe Left(DataNotFound)
    }
    "handle not saving the data" in {
      mockGetSessionData(taxYearEOY, aUser, aCisDeductions.employerRef, Right(Some(aCisUserData)))
      mockCreateOrUpdateCISUserData(taxYearEOY, aUser, aCisDeductions.employerRef, Left(DataNotUpdated))

      await(underTest.submitDeductionPeriod(taxYearEOY, aCisDeductions.employerRef, aUser, Month.JANUARY)) shouldBe Left(DataNotUpdated)
    }
  }
}
