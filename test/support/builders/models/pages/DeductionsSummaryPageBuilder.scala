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

package support.builders.models.pages

import models.pages.DeductionsSummaryPage
import support.TaxYearUtils
import support.builders.models.pages.elements.ContractorDeductionToDateBuilder.aContractorDeductionToDate

object DeductionsSummaryPageBuilder {

  val aDeductionsSummaryPage: DeductionsSummaryPage = DeductionsSummaryPage(
    taxYear = TaxYearUtils.taxYear,
    isInYear = true,
    gateway = true,
    deductions = Seq(
      aContractorDeductionToDate.copy(contractorName = Some("contractor-name-1"), employerRef = "ref-1", amount = Some(100.0)),
      aContractorDeductionToDate.copy(contractorName = Some("contractor-name-2"), employerRef = "ref-2", amount = Some(200.0))
    )
  )
}
