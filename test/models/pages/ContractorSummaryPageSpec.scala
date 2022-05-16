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

import support.builders.models.AllCISDeductionsBuilder.anAllCISDeductions
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.pages.ContractorSummaryPageBuilder.aContractorSummaryPage
import support.{TaxYearProvider, UnitTest}

import java.time.Month._

class ContractorSummaryPageSpec extends UnitTest
  with TaxYearProvider {

  ".isCustomerDeductionPeriod" should {
    "return true when given deduction period is in customerDeductionPeriods" in {
      val underTest = aContractorSummaryPage.copy(customerDeductionPeriods = Seq(APRIL, MAY))

      underTest.isCustomerDeductionPeriod(MAY) shouldBe true
    }

    "return false when given deduction period is not in customerDeductionPeriods" in {
      val underTest = aContractorSummaryPage.copy(customerDeductionPeriods = Seq(APRIL, MAY))

      underTest.isCustomerDeductionPeriod(JUNE) shouldBe false
    }
  }

  ".apply" should {
    "return a ContractorSummaryPage with a list of ordered months from contractor deductions when in year" in {
      val deductionDate1 = aPeriodData.copy(deductionPeriod = MARCH)
      val deductionDate2 = aPeriodData.copy(deductionPeriod = APRIL)
      val deductionDate3 = aPeriodData.copy(deductionPeriod = JUNE)
      val customerCisDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(periodData = Seq(deductionDate1)))))
      val contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(periodData = Seq(deductionDate2, deductionDate3)))))
      val allCISDeductions = anAllCISDeductions.copy(customerCISDeductions = customerCisDeductions, contractorCISDeductions = contractorCISDeductions)

      ContractorSummaryPage(
        taxYear = taxYear,
        isInYear = true,
        employerRef = "123/AB123456",
        incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))
      ) shouldBe aContractorSummaryPage.copy(
        taxYear = taxYear,
        isInYear = true,
        contractorName = Some("ABC SteelWorks"),
        employerRef = "123/AB123456",
        deductionPeriods = Seq(JUNE, APRIL),
        customerDeductionPeriods = Seq.empty
      )
    }

    "return a ContractorSummaryPage with a list of ordered months from all deductions when end of year" in {
      val deductionDate1 = aPeriodData.copy(deductionPeriod = MARCH)
      val deductionDate2 = aPeriodData.copy(deductionPeriod = APRIL)
      val deductionDate3 = aPeriodData.copy(deductionPeriod = JUNE)
      val deductionDate4 = aPeriodData.copy(deductionPeriod = JULY)
      val customerCisDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(periodData = Seq(deductionDate1, deductionDate4)))))
      val contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions.copy(periodData = Seq(deductionDate1, deductionDate2, deductionDate3)))))
      val allCISDeductions = anAllCISDeductions.copy(customerCISDeductions = customerCisDeductions, contractorCISDeductions = contractorCISDeductions)

      ContractorSummaryPage(
        taxYear = taxYearEOY,
        isInYear = false,
        employerRef = "123/AB123456",
        incomeTaxUserData = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))
      ) shouldBe aContractorSummaryPage.copy(
        taxYear = taxYearEOY,
        isInYear = false,
        contractorName = Some("ABC SteelWorks"),
        employerRef = "123/AB123456",
        deductionPeriods = Seq(JUNE, JULY, MARCH, APRIL),
        customerDeductionPeriods = Seq(JULY)
      )
    }
  }
}
