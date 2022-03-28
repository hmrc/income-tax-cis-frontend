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

import models.CisUserIsPriorSubmission
import models.mongo.{CisCYAModel, CisUserData, DataNotFoundError, DatabaseError}
import models.pages.ContractorDetailsViewModel
import org.joda.time.DateTime
import support.builders.models.UserBuilder._
import support.builders.models.mongo.CisCYAModelBuilder.aCisCYAModel
import support.builders.models.mongo.CisUserDataBuilder._
import support.mocks.MockCISUserDataRepository
import support.{TaxYearHelper, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TestingClock

import scala.concurrent.ExecutionContext

class ContractorDetailsServiceSpec extends UnitTest
  with MockCISUserDataRepository
  with TaxYearHelper {

  implicit private val hc: HeaderCarrier = HeaderCarrier()
  private val clock = TestingClock
  private val underTest = new ContractorDetailsService(mockRepo, clock)
  val dateTime = clock.now()
  implicit val ec = ExecutionContext.global

  val contractorDetailsViewModel = new ContractorDetailsViewModel("ABC Steelworks", "123/AB123456")
  val newCisUserData = CisUserData(
    aUser.sessionId,
    aUser.mtditid,
    aUser.nino,
    taxYear,
    contractorDetailsViewModel.employerReferenceNumber,
    None,
    false,
    (CisCYAModel(Some(contractorDetailsViewModel.contractorName), None)),
    DateTime.parse("2022-05-11T16:38:57.489Z")
  )

  ".checkAccessContractorDetailsPage" should {
    "return left with an error message " in {
      val error = DataNotFoundError
      mockFindCYAData(taxYear, aCisUserData.employerRef,  aUser, Left(error))
      await(underTest.checkAccessContractorDetailsPage(taxYear, aUser, aCisUserData.employerRef)) shouldBe Left(error)
    }

    "return left with an error message when there is a prior submission" in {
      mockFindCYAData(taxYear, aCisUserData.employerRef, aUser, Right(Some(aCisUserData)))
      await(underTest.checkAccessContractorDetailsPage(taxYear, aUser, aCisUserData.employerRef)) shouldBe Left(CisUserIsPriorSubmission)
    }

    "return right with user data when there is not a prior submission" in {
      mockFindCYAData(taxYear, aCisUserData.employerRef, aUser, Right(Some(aCisUserData.copy(isPriorSubmission = false))))
      await(underTest.checkAccessContractorDetailsPage(taxYear, aUser, aCisUserData.employerRef)) shouldBe Right(Some(aCisUserData.copy(isPriorSubmission = false)))
    }
  }

  ".createOrUpdateContractorDetails" should {
    "return left when find fails" in {
      val error = new DatabaseError {
        override val message: String = "Database broke"
      }
      mockFindCYAData(taxYearEOY, aCisUserData.employerRef, aUser, Left(error))
      await(underTest.createOrUpdateContractorDetails(contractorDetailsViewModel, taxYearEOY, aUser, None)) shouldBe Left(error)
    }

    "return left when createOrUpdate fails and Some data found" in {
      val error = new DatabaseError {
        override val message: String = "Database broke"
      }
      mockFindCYAData(taxYearEOY, aCisUserData.employerRef, aUser, Right(Some(aCisUserData.copy(isPriorSubmission = false))))
      mockCreateOrUpdateCYAData(aCisUserData.copy(isPriorSubmission = false), Left(error))
      await(underTest.createOrUpdateContractorDetails(contractorDetailsViewModel, taxYearEOY, aUser, None)) shouldBe Left(error)
    }

    "return Right when createOrUpdate succeeds and Some data found" in {
      mockFindCYAData(taxYearEOY, aCisUserData.employerRef, aUser, Right(Some(aCisUserData.copy(isPriorSubmission = false))))
      mockCreateOrUpdateCYAData(aCisUserData.copy(isPriorSubmission = false), Right())
      await(underTest.createOrUpdateContractorDetails(contractorDetailsViewModel, taxYearEOY, aUser, None)) shouldBe Right()
    }

    "return left when createOrUpdate fails and no data found" in {
      val error = new DatabaseError {
        override val message: String = "Database broke"
      }
      mockFindCYAData(taxYearEOY, aCisUserData.employerRef, aUser, Right(None))
      mockCreateOrUpdateCYAData(aCisUserData.copy(
        submissionId = None, isPriorSubmission = false, cis = aCisCYAModel.copy(periodData = None, priorPeriodData = Seq()), lastUpdated = dateTime),
        Left(error))
      await(underTest.createOrUpdateContractorDetails(contractorDetailsViewModel, taxYearEOY, aUser, None)) shouldBe Left(error)
    }

    "return Right when createOrUpdate succeeds and no data found" in {
      mockFindCYAData(taxYearEOY, aCisUserData.employerRef, aUser, Right(None))
      mockCreateOrUpdateCYAData(aCisUserData.copy(
        submissionId = None, isPriorSubmission = false, cis = aCisCYAModel.copy(periodData = None, priorPeriodData = Seq()), lastUpdated = dateTime),
        Right())
      await(underTest.createOrUpdateContractorDetails(contractorDetailsViewModel, taxYearEOY, aUser, None)) shouldBe Right()
    }
  }

}
