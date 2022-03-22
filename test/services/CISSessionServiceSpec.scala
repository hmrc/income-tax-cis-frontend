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

import models.mongo.{DataNotFoundError, DataNotUpdatedError}
import models.{APIErrorBodyModel, APIErrorModel, EmptyPriorCisDataError, HttpParserError}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder.aUser
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockCISUserDataRepository, MockIncomeTaxUserDataConnector}
import support.{TaxYearHelper, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestingClock

import scala.concurrent.ExecutionContext.Implicits.global

class CISSessionServiceSpec extends UnitTest
  with MockIncomeTaxUserDataConnector
  with MockCISUserDataRepository
  with TaxYearHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()

  private val underTest = new CISSessionService(
    mockRepo,
    mockIncomeTaxUserDataConnector,
    TestingClock
  )

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
        aCisUserData.isPriorSubmission, aCisCYAModel)) shouldBe Left(())
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

  ".getPriorAndMakeCYA" should {
    "return data" in {
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData))
      val cya = anIncomeTaxUserData.getCISDeductionsFor(aCisDeductions.employerRef).get.toCYA
      mockCreateOrUpdateCYAData(aCisUserData.copy(cis = cya, lastUpdated = TestingClock.now()), Right(()))

      await(underTest.getPriorAndMakeCYA(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Right(aCisUserData.copy(cis = cya, lastUpdated = TestingClock.now()))
    }
    "handle when no data for the employer ref" in {
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData.copy(cis = None)))

      await(underTest.getPriorAndMakeCYA(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Left(EmptyPriorCisDataError)
    }
    "handle when saving the data fails" in {
      mockGetUserData(aUser.nino, taxYearEOY, Right(anIncomeTaxUserData))
      val cya = anIncomeTaxUserData.getCISDeductionsFor(aCisDeductions.employerRef).get.toCYA
      mockCreateOrUpdateCYAData(aCisUserData.copy(cis = cya, lastUpdated = TestingClock.now()), Left(DataNotUpdatedError))

      await(underTest.getPriorAndMakeCYA(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Left(DataNotUpdatedError)
    }
    "handle error from getting data" in {
      mockGetUserData(aUser.nino, taxYearEOY, Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.getPriorAndMakeCYA(taxYearEOY, aCisDeductions.employerRef, aUser)) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }
  }
}
