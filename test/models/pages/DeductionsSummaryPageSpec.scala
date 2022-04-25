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

import models.AllCISDeductions
import models.pages.elements.ContractorDeductionToDate
import support.UnitTest
import support.builders.models.CISSourceBuilder.aCISSource
import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.IncomeTaxUserDataBuilder.anIncomeTaxUserData

class DeductionsSummaryPageSpec extends UnitTest {

  private val anyTaxYear: Int = 2020

  "DeductionsSummaryPage.mapToInYearPage" should {
    "map to page with empty deductions when cis is None" in {
      DeductionsSummaryPage.mapToInYearPage(anyTaxYear, anIncomeTaxUserData.copy(cis = None)) shouldBe
        DeductionsSummaryPage(anyTaxYear, isInYear = true, Seq.empty)
    }

    "map to page with in year deductions only" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"))
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"))
      val allCISDeductions = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      val data = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      DeductionsSummaryPage.mapToInYearPage(anyTaxYear, data) shouldBe DeductionsSummaryPage(anyTaxYear, isInYear = true, Seq(
        ContractorDeductionToDate(aCisDeductions1.contractorName, aCisDeductions1.employerRef, aCisDeductions1.totalDeductionAmount),
        ContractorDeductionToDate(aCisDeductions2.contractorName, aCisDeductions2.employerRef, aCisDeductions2.totalDeductionAmount)
      ))
    }

    "map to page with end of year deductions" in {
      val aCisDeductions1 = aCisDeductions.copy(contractorName = Some("contractor-1"), employerRef = "12345")
      val aCisDeductions2 = aCisDeductions.copy(contractorName = Some("contractor-2"), employerRef = "54321")
      val allCISDeductions = AllCISDeductions(
        customerCISDeductions = Some(aCISSource),
        contractorCISDeductions = Some(aCISSource.copy(cisDeductions = Seq(aCisDeductions1, aCisDeductions2)))
      )

      val data = anIncomeTaxUserData.copy(cis = Some(allCISDeductions))

      DeductionsSummaryPage.mapToEndOfYearPage(anyTaxYear, data) shouldBe DeductionsSummaryPage(anyTaxYear, isInYear = false, Seq(
        ContractorDeductionToDate(aCisDeductions.contractorName, aCisDeductions.employerRef, aCisDeductions.totalDeductionAmount),
        ContractorDeductionToDate(aCisDeductions1.contractorName, aCisDeductions1.employerRef, aCisDeductions1.totalDeductionAmount),
        ContractorDeductionToDate(aCisDeductions2.contractorName, aCisDeductions2.employerRef, aCisDeductions2.totalDeductionAmount)
      ))
    }
  }
}
