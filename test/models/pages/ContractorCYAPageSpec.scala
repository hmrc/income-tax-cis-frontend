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

package models.pages

import support.UnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.mongo.CisUserDataBuilder.aCisUserData
import support.builders.models.pages.ContractorCYAPageBuilder.aContractorCYAPage

import java.time.Month

class ContractorCYAPageSpec extends UnitTest {

  private val anyTaxYear: Int = 2022

  ".hasPaidForMaterials" should {
    "return true when costOfMaterials has value" in {
      val underTest = aContractorCYAPage.copy(costOfMaterials = Some(100))

      underTest.hasPaidForMaterials shouldBe true
    }

    "return false when costOfMaterials is None" in {
      val underTest = aContractorCYAPage.copy(costOfMaterials = None)

      underTest.hasPaidForMaterials shouldBe false
    }
  }

  "ContractorCYAPage.mapToInYearPage" should {
    "map to page with in year deductions" in {
      val periodData = Seq(
        aPeriodData.copy(deductionPeriod = Month.MAY),
        aPeriodData.copy(deductionPeriod = Month.JUNE, grossAmountPaid = Some(100.0), deductionAmount = Some(200.0), costOfMaterials = Some(300.0))
      )
      val cisDeductions = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "ref-1", periodData = periodData)

      ContractorCYAPage.inYearMapToPageModel(anyTaxYear, cisDeductions, Month.JUNE, isAgent = false) shouldBe ContractorCYAPage(
        taxYear = anyTaxYear,
        isInYear = true,
        contractorName = Some("contractor-1"),
        employerRef = "ref-1",
        month = Month.JUNE,
        labourAmount = Some(100.0),
        deductionAmount = Some(200.0),
        costOfMaterials = Some(300.0),
        isPriorSubmission = true,
        isContractorDeduction = true,
        isAgent = false
      )
    }
  }

  "ContractorCYAPage.eoyMapToPageModel" should {
    "map to page with end of year year deductions" in {

      ContractorCYAPage.eoyMapToPageModel(anyTaxYear, aCisUserData, isAgent = false) shouldBe ContractorCYAPage(
        taxYear = anyTaxYear,
        isInYear = false,
        contractorName = Some("ABC Steelworks"),
        employerRef = "123/AB123456",
        month = Month.MAY,
        labourAmount = Some(500.0),
        deductionAmount = Some(100.0),
        costOfMaterials = Some(250.0),
        isPriorSubmission = true,
        isContractorDeduction = false,
        isAgent = false
      )
    }
  }
}