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

import models.tailoring.{ExcludeJourneyModel, ExcludedJourneysResponseModel}
import models.{FailedTailoringOverrideDeductionError, FailedTailoringRemoveDeductionError, HttpParserError, InvalidOrUnfinishedSubmission}
import support.builders.models.IncomeTaxUserDataBuilder
import support.builders.models.UserBuilder.aUser
import support.mocks.{MockCISSessionService, MockContractorCYAService, MockDeleteCISPeriodService, MockTailoringConnector}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class TailoringServiceSpec extends UnitTest
  with TaxYearProvider
  with MockTailoringConnector
  with MockCISSessionService
  with MockContractorCYAService
  with MockDeleteCISPeriodService {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val underTest: TailoringService = new TailoringService(
    mockService,
    mockCISSessionService,
    mockContractorCYAService,
    mockTailoringDataConnector
  )(ExecutionContext.global)


  ".removeCISData" should {
    "return a successful result" in {
          mockRemoveCISDeductionTailoring(taxYear, aUser, IncomeTaxUserDataBuilder.anIncomeTaxUserData, Right((): Unit))
          mockSubmitCisDeductionCYATailoring(taxYear, aUser, Right((): Unit))
          val response = underTest.removeCISData(taxYear, aUser, IncomeTaxUserDataBuilder.anIncomeTaxUserData)
          await(response) shouldBe Right((): Unit)
    }
    "return an error result when remove fails" in {
          mockRemoveCISDeductionTailoring(taxYear, aUser, IncomeTaxUserDataBuilder.anIncomeTaxUserData, Left(InvalidOrUnfinishedSubmission))
          mockSubmitCisDeductionCYATailoring(taxYear, aUser, Right((): Unit))
          val response = underTest.removeCISData(taxYear, aUser, IncomeTaxUserDataBuilder.anIncomeTaxUserData)
          await(response) shouldBe Left(FailedTailoringRemoveDeductionError)
    }
    "return an error result when update fails" in {
          mockRemoveCISDeductionTailoring(taxYear, aUser, IncomeTaxUserDataBuilder.anIncomeTaxUserData, Right((): Unit))
          mockSubmitCisDeductionCYATailoring(taxYear, aUser, Left(HttpParserError(500)))
          val response = underTest.removeCISData(taxYear, aUser, IncomeTaxUserDataBuilder.anIncomeTaxUserData)
          await(response) shouldBe Left(FailedTailoringOverrideDeductionError)
    }
  }
  ".getExcludedJourneys" should {
    "return a successful result" in {

          mockGetExcludedJourneys(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("cis", None))), taxYear,"nino")
          val response = underTest.getExcludedJourneys(taxYear, "nino", "1234567890")

          await(response) shouldBe Right(ExcludedJourneysResponseModel(Seq(ExcludeJourneyModel("cis", None))))
    }
  }
  ".clearExcludedJourneys" should {
    "return a successful result" in {

          mockClearExcludedJourneys(taxYear, "nino")
          val response = underTest.clearExcludedJourney(taxYear, "nino", "1234567890")

          await(response) shouldBe Right(true)
    }
  }
  ".postExcludedJourneys" should {
    "return a successful result" in {

          mockPostExcludedJourneys(taxYear, "nino")
          val response = underTest.postExcludedJourney(taxYear, "nino", "1234567890")

          await(response) shouldBe Right(true)
    }
  }
}
