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

import models.HttpParserError
import models.forms.ContractorDetails
import models.mongo.{CisCYAModel, DataNotUpdatedError}
import play.api.http.Status.INTERNAL_SERVER_ERROR
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.UserBuilder._
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.mocks.{MockCISSessionService, MockCISUserDataRepository}
import support.{TaxYearProvider, UnitTest}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class ContractorDetailsServiceSpec extends UnitTest
  with MockCISSessionService
  with MockCISUserDataRepository
  with TaxYearProvider {

  private val formData = new ContractorDetails(contractorName = "some-name", employerReferenceNumber = "some-ref")

  private val underTest = new ContractorDetailsService(mockCISSessionService, mockCisUserDataRepository)

  ".getPriorEmployerRefs" should {
    "return employer refs from prior" in {
      mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData))

      await(underTest.getPriorEmployerRefs(taxYearEOY, aUser)(HeaderCarrier())) shouldBe Right(Seq(aCisDeductions.employerRef))
    }
    "return an empty seq when no data" in {
      mockGetPriorData(taxYearEOY, aUser, Right(anIncomeTaxUserData.copy(cis = None)))

      await(underTest.getPriorEmployerRefs(taxYearEOY, aUser)(HeaderCarrier())) shouldBe Right(Seq())
    }
    "return an error when call fails" in {
      mockGetPriorData(taxYearEOY, aUser, Left(HttpParserError(INTERNAL_SERVER_ERROR)))

      await(underTest.getPriorEmployerRefs(taxYearEOY, aUser)(HeaderCarrier())) shouldBe Left(HttpParserError(INTERNAL_SERVER_ERROR))
    }
  }

  ".saveContractorDetails" should {
    "return DataNotUpdatedError when cisSessionService.createOrUpdateCISUserData returns error" in {
      val newCisCYAModel = aCisUserData.cis.copy(contractorName = Some(formData.contractorName))

      mockClear(taxYear, aCisUserData.employerRef, result = true)
      mockCreateOrUpdateCISUserData(taxYear, aUser, employerRef = "some-ref", aCisUserData.submissionId, aCisUserData.isPriorSubmission, newCisCYAModel, Left(DataNotUpdatedError))

      await(underTest.saveContractorDetails(taxYear, aUser, Some(aCisUserData), formData)) shouldBe Left(DataNotUpdatedError)
    }

    "return DataNotUpdatedError when clear returns error" in {
      mockClear(taxYear, aCisUserData.employerRef, result = false)

      await(underTest.saveContractorDetails(taxYear, aUser, Some(aCisUserData), formData)) shouldBe Left(DataNotUpdatedError)
    }

    "persist updated CisUserData when previous exists when" when {
      "employerRef updated" in {
        val newCisCYAModel = aCisUserData.cis.copy(contractorName = Some(formData.contractorName))

        mockClear(taxYear, aCisUserData.employerRef, result = true)
        mockCreateOrUpdateCISUserData(taxYear, aUser, employerRef = "some-ref", aCisUserData.submissionId, aCisUserData.isPriorSubmission, newCisCYAModel, Right(aCisUserData))

        await(underTest.saveContractorDetails(taxYear, aUser, Some(aCisUserData), formData)) shouldBe Right(aCisUserData)
      }

      "employerRef is not updated" in {
        val formDataWithSameEmployerRef = formData.copy(employerReferenceNumber = aCisUserData.employerRef)
        val newCisCYAModel = aCisUserData.cis.copy(contractorName = Some(formDataWithSameEmployerRef.contractorName))

        mockCreateOrUpdateCISUserData(taxYear, aUser, employerRef = aCisUserData.employerRef, aCisUserData.submissionId, aCisUserData.isPriorSubmission, newCisCYAModel, Right(aCisUserData))

        await(underTest.saveContractorDetails(taxYear, aUser, Some(aCisUserData), formDataWithSameEmployerRef)) shouldBe Right(aCisUserData)
      }
    }

    "persist updated CisUserData when previous does not exists" in {
      val newCisCYAModel = CisCYAModel(contractorName = Some(formData.contractorName))

      mockCreateOrUpdateCISUserData(taxYear, aUser, employerRef = "some-ref", None, isPriorSubmission = false, newCisCYAModel, Right(aCisUserData))

      await(underTest.saveContractorDetails(taxYear, aUser, None, formData)) shouldBe Right(aCisUserData)
    }
  }
}
