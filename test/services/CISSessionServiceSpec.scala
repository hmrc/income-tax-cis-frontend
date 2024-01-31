/*
 * Copyright 2024 HM Revenue & Customs
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

import models.mongo.{DataNotFoundError, DataNotUpdatedError}
import models.{APIErrorBodyModel, APIErrorModel, HttpParserError}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockCISUserDataRepository, MockIncomeTaxUserDataConnector, MockRefreshIncomeSourceConnector}
import support.{FakeRequestHelper, TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestingClock

import java.time.Month
import scala.concurrent.ExecutionContext.Implicits.global

class CISSessionServiceSpec extends UnitTest
  with MockIncomeTaxUserDataConnector
  with MockRefreshIncomeSourceConnector
  with MockCISUserDataRepository
  with TaxYearProvider
  with FakeRequestHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new CISSessionService(
    mockCisUserDataRepository,
    mockIncomeTaxUserDataConnector,
    mockRefreshIncomeSourceConnector,
    TestingClock
  )

  ".refreshAndClear" should {

    "return right when refreshes and clears data" in {
      mockClear(taxYearEOY, aCisDeductions.employerRef, result = true)
      mockRefresh(aUser.nino, taxYearEOY, Right(()))

      await(underTest.refreshAndClear(aUser, aCisDeductions.employerRef, taxYearEOY)) shouldBe Right(())
    }
    "return right when refreshes data" in {
      mockRefresh(aUser.nino, taxYearEOY, Right(()))

      await(underTest.refreshAndClear(aUser, aCisDeductions.employerRef, taxYearEOY, clearCYA = false)) shouldBe Right(())
    }
    "return error when fails to clear data" in {
      mockClear(taxYearEOY, aCisDeductions.employerRef, result = false)
      mockRefresh(aUser.nino, taxYearEOY, Right(()))

      await(underTest.refreshAndClear(aUser, aCisDeductions.employerRef, taxYearEOY)) shouldBe Left(DataNotUpdatedError)
    }
    "return error when fails to refresh data" in {
      mockRefresh(aUser.nino, taxYearEOY, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.refreshAndClear(aUser, aCisDeductions.employerRef, taxYearEOY)) shouldBe Left(HttpParserError(500))
    }
  }

  ".getSessionData" should {

    "return error when fails to get data" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Left(DataNotFoundError))

      await(underTest.getSessionData(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Left(DataNotFoundError)
    }
    "return data" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aCisUserData)))

      await(underTest.getSessionData(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Right(Some(aCisUserData))
    }
  }

  ".createOrUpdateCISUserData" should {
    "submit and save the data" in {
      mockCreateOrUpdateCYAData(aCisUserData.copy(lastUpdated = TestingClock.now()), Right(()))

      await(underTest.createOrUpdateCISUserData(aUser, taxYearEOY, aCisUserData.employerRef, aCisUserData.submissionId,
        aCisUserData.isPriorSubmission, aCisCYAModel)) shouldBe Right(aCisUserData.copy(lastUpdated = TestingClock.now()))
    }
    "handle an error" in {
      mockCreateOrUpdateCYAData(aCisUserData.copy(lastUpdated = TestingClock.now()), Left(DataNotUpdatedError))

      await(underTest.createOrUpdateCISUserData(aUser, taxYearEOY, aCisUserData.employerRef, aCisUserData.submissionId,
        aCisUserData.isPriorSubmission, aCisCYAModel)) shouldBe Left(DataNotUpdatedError)
    }
  }

  ".getPriorData" should {
    "return error when fails to get data" in {
      mockGetUserData(aUser.nino, taxYearEOY, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.getPriorData(aUser, taxYearEOY)) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }
    "return data" in {
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData))

      await(underTest.getPriorData(aUser, taxYearEOY)) shouldBe Right(anIncomeTaxUserData)
    }
  }

  ".checkCyaAndReturnData" should {
    val anyBoolean = true
    "return data" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(None))
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData))
      val cya = anIncomeTaxUserData.eoyCisDeductionsWith(aCisDeductions.employerRef).get.toCYA(Some(Month.MAY), Seq(Month.MAY), anyBoolean)
      mockCreateOrUpdateCYAData(aCisUserData.copy(cis = cya, lastUpdated = TestingClock.now()), Right(()))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Right(Some(aCisUserData.copy(cis = cya, lastUpdated = TestingClock.now())))
    }
    "return session data" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aCisUserData)))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Right(Some(aCisUserData))
    }
    "return session data for new submission" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(Some(aCisUserData.copy(cis = aCisCYAModel.copy(
        periodData = Some(aCisCYAModel.periodData.get.copy(originallySubmittedPeriod = None)), priorPeriodData = Seq.empty)))))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Right(Some(aCisUserData.copy(cis = aCisCYAModel.copy(
        periodData = Some(aCisCYAModel.periodData.get.copy(originallySubmittedPeriod = None)), priorPeriodData = Seq.empty))))
    }

    "handle when no data for the employer ref" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(None))
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData.copy(cis = None)))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Right(None)
    }

    "handle when saving the data fails" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(None))
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData))
      val cya = anIncomeTaxUserData.eoyCisDeductionsWith(aCisDeductions.employerRef).get.toCYA(Some(Month.MAY), Seq(Month.MAY), anyBoolean)
      mockCreateOrUpdateCYAData(aCisUserData.copy(cis = cya, lastUpdated = TestingClock.now()), Left(DataNotUpdatedError))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Left(DataNotUpdatedError)
    }

    "handle error from getting data" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Right(None))
      mockGetUserData(aUser.nino, taxYearEOY, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }
    "handle error from getting session data" in {
      mockFindCYAData(taxYearEOY, aCisDeductions.employerRef, aUser, Left(DataNotFoundError))

      await(underTest.checkCyaAndReturnData(taxYearEOY, aCisDeductions.employerRef, aUser, Month.MAY)(hc)) shouldBe Left(DataNotFoundError)
    }
  }
}
