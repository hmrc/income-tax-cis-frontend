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

import models.mongo.DataNotUpdatedError
import models.{APIErrorBodyModel, APIErrorModel, HttpParserError, InvalidOrUnfinishedSubmission}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.CISSubmissionBuilder.{aPeriodData, anUpdateCISSubmission}
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.UserBuilder._
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockCISConnector, MockCISSessionService}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ContractorCYAServiceSpec extends UnitTest
  with MockCISSessionService
  with MockCISConnector
  with TaxYearProvider {

  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> aUser.mtditid)

  private val underTest = new ContractorCYAService(mockCISSessionService,mockCISConnector)

  ".submitCisDeductionCYA" should {
    "return right when submit and clear are successful" in {

      mockSubmit(aUser.nino, taxYearEOY, anUpdateCISSubmission.copy(
        periodData = Seq(aPeriodData,aPeriodData.copy(deductionFromDate = "2021-10-06",deductionToDate = "2021-11-05"))
      ), Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Right(()))

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData)) shouldBe Right(())
    }
    "return error when clear fails" in {

      mockSubmit(aUser.nino, taxYearEOY, anUpdateCISSubmission.copy(
        periodData = Seq(aPeriodData,aPeriodData.copy(deductionFromDate = "2021-10-06",deductionToDate = "2021-11-05"))
      ), Right(()))
      mockRefreshAndClear(taxYearEOY, aCisUserData.employerRef, result = Left(DataNotUpdatedError))

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData)) shouldBe Left(DataNotUpdatedError)
    }
    "return error when submit fails" in {

      mockSubmit(aUser.nino, taxYearEOY, anUpdateCISSubmission.copy(
        periodData = Seq(aPeriodData,aPeriodData.copy(deductionFromDate = "2021-10-06",deductionToDate = "2021-11-05"))
      ),  Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError)))

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData)) shouldBe Left(HttpParserError(500))
    }
    "return InvalidOrUnfinishedSubmission error unable to submit" in {

      await(underTest.submitCisDeductionCYA(taxYearEOY, aCisDeductions.employerRef, aUser, aCisUserData.copy(cis =
        aCisCYAModel.copy(contractorName = None),
        submissionId = None
      ))) shouldBe Left(InvalidOrUnfinishedSubmission)
    }
  }
}
