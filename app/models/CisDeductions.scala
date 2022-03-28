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

package models

import models.mongo.{CYAPeriodData, CisCYAModel}
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

  val submissionId: Option[String] =
    periodData.find(_.submissionId.isDefined).flatMap(_.submissionId)

  def periodDataFor(month: Month): Option[PeriodData] =
    periodData.find(_.deductionPeriod == month)

  def toCYA: CisCYAModel = {
    val periods = periodData.map { period =>
      CYAPeriodData(
        period.deductionPeriod,
        grossAmountPaid = period.grossAmountPaid,
        deductionAmount = period.deductionAmount,
        costOfMaterialsQuestion = Some(period.costOfMaterials.isDefined),
        costOfMaterials = period.costOfMaterials
      )
    }

    CisCYAModel(
      contractorName = contractorName,
      periodData = None,
      priorPeriodData = periods
    )
  }
}

object CisDeductions {
  implicit val format: OFormat[CisDeductions] = Json.format[CisDeductions]
}
