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

import java.time.Month

import support.builders.models.CisDeductionsBuilder.aCisDeductions
import support.builders.models.PeriodDataBuilder.aPeriodData
import support.builders.models.pages.ContractorSummaryPageBuilder.aContractorSummaryPage
import support.{TaxYearProvider, UnitTest}

class ContractorSummaryPageSpec extends UnitTest with TaxYearProvider {

  ".mapToInYearPage" should {
    "return a ContractorSummaryPage with a list of ordered months" in {
      val deductionDate1 = aPeriodData.copy(deductionPeriod = Month.MARCH)
      val deductionDate2 = aPeriodData.copy(deductionPeriod = Month.APRIL)
      val deductionDate3 = aPeriodData.copy(deductionPeriod = Month.JUNE)

      ContractorSummaryPage.mapToInYearPage(
        taxYear = taxYear,
        cisDeductions = aCisDeductions.copy(periodData = Seq(deductionDate1, deductionDate2, deductionDate3))
      ) shouldBe aContractorSummaryPage.copy(
        taxYear = taxYear,
        contractorName = Some("ABC SteelWorks"),
        employerRef = "123/AB123456",
        deductionPeriods = Seq(Month.JUNE, Month.MARCH, Month.APRIL)
      )


    }
  }

}
