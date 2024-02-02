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

package models

import models.mongo.{CYAPeriodData, CisCYAModel}
import models.submission.CISSubmission
import play.api.libs.json.{Json, OFormat}

import java.time.Month

case class CisDeductions(fromDate: String,
                         toDate: String,
                         contractorName: Option[String],
                         employerRef: String,
                         totalDeductionAmount: Option[BigDecimal],
                         totalCostOfMaterials: Option[BigDecimal],
                         totalGrossAmountPaid: Option[BigDecimal],
                         periodData: Seq[PeriodData]) {
  def toCISSubmission(taxYear: Int): CISSubmission = {
    CISSubmission(
      employerRef = Some(employerRef),
      contractorName = contractorName,
      periodData = this.periodData.map(_.toZeroSubmissionPeriodData(taxYear)),
      submissionId = this.periodData.map(_.submissionId).head
    )
  }

  private def isLaterInTaxYear(p: PeriodData): Boolean = p.deductionPeriod == Month.JANUARY ||
    p.deductionPeriod == Month.FEBRUARY ||
    p.deductionPeriod == Month.MARCH ||
    p.deductionPeriod == Month.APRIL

  def withSortedPeriodData: CisDeductions = {
    val periodsEarlierInTaxYear = periodData.filterNot(isLaterInTaxYear).sortBy(_.deductionPeriod)
    val periodsLaterInTaxYear = periodData.filter(isLaterInTaxYear).sortBy(_.deductionPeriod)

    this.copy(
      periodData = periodsEarlierInTaxYear ++ periodsLaterInTaxYear
    )
  }

  def recalculateFigures: CisDeductions = {
    this.copy(
      totalDeductionAmount = Some(periodData.flatMap(_.deductionAmount).sum),
      totalCostOfMaterials = if (periodData.flatMap(_.costOfMaterials).sum > BigDecimal(0)) Some(periodData.flatMap(_.costOfMaterials).sum) else None,
      totalGrossAmountPaid = Some(periodData.flatMap(_.grossAmountPaid).sum)
    )
  }

  val submissionId: Option[String] =
    periodData.find(_.submissionId.isDefined).flatMap(_.submissionId)

  def periodDataFor(month: Month): Option[PeriodData] =
    periodData.find(_.deductionPeriod == month)

  def toCYA(month: Option[Month], contractorSubmittedMonths: Seq[Month], hasCompleted: Boolean): CisCYAModel = {
    val periods = periodData.map { period =>
      CYAPeriodData(
        period.deductionPeriod,
        grossAmountPaid = period.grossAmountPaid,
        deductionAmount = period.deductionAmount,
        costOfMaterialsQuestion = Some(period.costOfMaterials.isDefined),
        costOfMaterials = period.costOfMaterials,
        contractorSubmitted = contractorSubmittedMonths.contains(period.deductionPeriod),
        originallySubmittedPeriod = Some(period.deductionPeriod)
      )
    }

    CisCYAModel(
      contractorName = contractorName,
      periodData = if (month.isDefined) periods.find(_.deductionPeriod == month.get) else None,
      priorPeriodData = periods
    )
  }
}

object CisDeductions {
  implicit val format: OFormat[CisDeductions] = Json.format[CisDeductions]
}
