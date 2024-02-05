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

import models.AllCISDeductions
import support.UnitTest
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import support.builders.models.pages.elements.ContractorDeductionToDateBuilder.aContractorDeductionToDate

class DeductionsSummaryPageSpec extends UnitTest {

  private val anyTaxYear: Int = 2020
  private val anyBoolean = true

  ".apply(...)" should {
    "return page with empty deductions when cis is None" in {
      DeductionsSummaryPage.apply(anyTaxYear, isInYear = anyBoolean, gateway = true, anIncomeTaxUserData.copy(cis = None)) shouldBe
        DeductionsSummaryPage(anyTaxYear, isInYear = anyBoolean, gateway = true, Seq.empty)
    }

    "return a page with in year deductions only when in year" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"))
      val allCISDeductions = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      DeductionsSummaryPage.apply(anyTaxYear, isInYear = true, gateway = true, anIncomeTaxUserData.copy(cis = Some(allCISDeductions))) shouldBe DeductionsSummaryPage(
        anyTaxYear,
        isInYear = true,
        gateway = true,
        deductions = Seq(
          aContractorDeductionToDate.copy(aCisDeductions1.contractorName, aCisDeductions1.employerRef, aCisDeductions1.totalDeductionAmount),
          aContractorDeductionToDate.copy(aCisDeductions2.contractorName, aCisDeductions2.employerRef, aCisDeductions2.totalDeductionAmount)
        )
      )
    }

    "return a page with end of year deductions when end of year" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "12345")
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "54321")
      val allCISDeductions = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      DeductionsSummaryPage.apply(anyTaxYear, isInYear = false, gateway = true, anIncomeTaxUserData.copy(cis = Some(allCISDeductions))) shouldBe DeductionsSummaryPage(
        anyTaxYear,
        isInYear = false,
        gateway = true,
        deductions = Seq(
          aContractorDeductionToDate.copy(aCisDeductions.contractorName, aCisDeductions.employerRef, aCisDeductions.totalDeductionAmount),
          aContractorDeductionToDate.copy(aCisDeductions1.contractorName, aCisDeductions1.employerRef, aCisDeductions1.totalDeductionAmount),
          aContractorDeductionToDate.copy(aCisDeductions2.contractorName, aCisDeductions2.employerRef, aCisDeductions2.totalDeductionAmount)
        )
      )
    }
  }
}
