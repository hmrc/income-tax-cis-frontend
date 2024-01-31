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

import models.IncomeTaxUserData
import models.pages.elements.ContractorDeductionToDate

case class DeductionsSummaryPage(taxYear: Int,
                                 isInYear: Boolean,
                                 gateway: Boolean,
                                 deductions: Seq[ContractorDeductionToDate])

object DeductionsSummaryPage {

  def apply(taxYear: Int,
            isInYear: Boolean,
            gateway: Boolean,
            incomeTaxUserData: IncomeTaxUserData): DeductionsSummaryPage = DeductionsSummaryPage(
    taxYear = taxYear,
    isInYear = isInYear,
    gateway = gateway,
    deductions(incomeTaxUserData, inYear = isInYear)
  )

  private def deductions(incomeTaxUserData: IncomeTaxUserData, inYear: Boolean): Seq[ContractorDeductionToDate] = {
    incomeTaxUserData.cis
      .map(cis => if (inYear) cis.inYearCisDeductions else cis.endOfYearCisDeductions)
      .getOrElse(Seq.empty)
      .map(ContractorDeductionToDate(_))
  }
}
