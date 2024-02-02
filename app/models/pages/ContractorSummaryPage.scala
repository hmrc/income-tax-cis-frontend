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

import java.time.Month
import java.time.Month.APRIL

case class ContractorSummaryPage(taxYear: Int,
                                 isInYear: Boolean,
                                 contractorName: Option[String],
                                 employerRef: String,
                                 deductionPeriods: Seq[Month],
                                 customerDeductionPeriods: Seq[Month]) {

  def isCustomerDeductionPeriod(month: Month): Boolean =
    customerDeductionPeriods.contains(month)
}

object ContractorSummaryPage {

  def apply(taxYear: Int,
            isInYear: Boolean,
            employerRef: String,
            incomeTaxUserData: IncomeTaxUserData): ContractorSummaryPage = {

    val cisDeductions = if (isInYear) incomeTaxUserData.inYearCisDeductionsWith(employerRef).get else incomeTaxUserData.eoyCisDeductionsWith(employerRef).get
    val deductionPeriods = cisDeductions.periodData.map(_.deductionPeriod)
    val months = Month.values().toIndexedSeq
    val monthsOrdered: Seq[Month] = months.drop(APRIL.getValue) ++ months.take(APRIL.getValue)
    val orderedDeductionPeriods = monthsOrdered.foldLeft(Seq[Month]())((list, h) => if (deductionPeriods.contains(h)) h +: list else list).reverse
    val customerDeductionPeriods = if (isInYear) Seq.empty else getCustomerDeductionPeriods(employerRef, incomeTaxUserData)

    ContractorSummaryPage(
      taxYear = taxYear,
      isInYear = isInYear,
      contractorName = cisDeductions.contractorName,
      employerRef = employerRef,
      deductionPeriods = orderedDeductionPeriods,
      customerDeductionPeriods = customerDeductionPeriods
    )
  }

  private def getCustomerDeductionPeriods(employerRef: String, incomeTaxUserData: IncomeTaxUserData): Seq[Month] = {
    val hmrcDeductionPeriods = incomeTaxUserData.inYearPeriodDataWith(employerRef).map(_.deductionPeriod)
    val customerCisDeductions = incomeTaxUserData.customerCisDeductionsWith(employerRef).map(_.periodData.map(_.deductionPeriod)).getOrElse(Seq.empty)

    customerCisDeductions.foldLeft(Seq[Month]())((acc, i) => if (hmrcDeductionPeriods.contains(i)) acc else i +: acc)
  }
}
