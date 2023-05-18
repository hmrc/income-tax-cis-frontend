/*
 * Copyright 2023 HM Revenue & Customs
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

import models.{CisDeductions, IncomeTaxUserData, PeriodData}

import java.time.Month.APRIL
import java.time.{LocalDate, Month}

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

    val dateNow: LocalDate = LocalDate.now()
    val taxYearCutoffDate: LocalDate = LocalDate.parse(s"${dateNow.getYear}-04-05")

    val taxYear: Int = if (dateNow.isAfter(taxYearCutoffDate)) dateNow.getYear + 1 else dateNow.getYear
    val taxYearEOY: Int = taxYear - 1

    val aPeriodData: PeriodData = PeriodData(
      deductionPeriod = Month.MAY,
      deductionAmount = Some(100.00),
      costOfMaterials = Some(50.00),
      grossAmountPaid = Some(450.00),
      submissionDate = s"${taxYearEOY - 2}-05-11T16:38:57.489Z",
      submissionId = Some("submissionId"),
      source = "customer"
    )

    val cisDeductions = CisDeductions(
      fromDate = "2020-04-06",
      toDate = "2021-04-05",
      contractorName = Some("Michele Lamy Paving Limited"),
      employerRef = "123/AB123456",
      totalDeductionAmount = Some(100.00),
      totalCostOfMaterials = Some(50.00),
      totalGrossAmountPaid = Some(450.00),
      periodData = Seq(aPeriodData,
        aPeriodData.copy(deductionPeriod = Month.JUNE),
        aPeriodData.copy(deductionPeriod = Month.JULY))
    )


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
