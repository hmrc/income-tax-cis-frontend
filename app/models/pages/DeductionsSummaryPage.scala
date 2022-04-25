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

import models.IncomeTaxUserData
import models.pages.elements.ContractorDeductionToDate

case class DeductionsSummaryPage(taxYear: Int,
                                 isInYear: Boolean,
                                 deductions: Seq[ContractorDeductionToDate])

object DeductionsSummaryPage {

  private def deductions(incomeTaxUserData: IncomeTaxUserData, inYear: Boolean): Seq[ContractorDeductionToDate] ={
    incomeTaxUserData.cis
      .map(cis => if(inYear) cis.inYearCisDeductions else cis.endOfYearCisDeductions)
      .getOrElse(Seq.empty)
      .map(ContractorDeductionToDate(_))
  }

  def mapToInYearPage(taxYear: Int, incomeTaxUserData: IncomeTaxUserData): DeductionsSummaryPage = {
    DeductionsSummaryPage(taxYear, isInYear = true, deductions(incomeTaxUserData, inYear = true))
  }

  def mapToEndOfYearPage(taxYear: Int, incomeTaxUserData: IncomeTaxUserData): DeductionsSummaryPage = {
    DeductionsSummaryPage(taxYear, isInYear = false, deductions(incomeTaxUserData, inYear = false))
  }
}
