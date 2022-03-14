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

package models.pages

import support.ControllerUnitTest
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.pages.ContractorCYAPageBuilder.aContractorCYAPage

import java.time.Month

class ContractorCYAPageSpec extends ControllerUnitTest {

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

      ContractorCYAPage.mapToInYearPage(taxYear, cisDeductions, Month.JUNE) shouldBe ContractorCYAPage(
        taxYear = taxYear,
        isInYear = true,
        contractorName = Some("contractor-1"),
        employerRef = "ref-1",
        month = Month.JUNE,
        labourAmount = Some(100.0),
        deductionAmount = Some(200.0),
        costOfMaterials = Some(300.0)
      )
    }
  }
}