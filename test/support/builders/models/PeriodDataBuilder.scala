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

package support.builders.models

import models.PeriodData
import play.api.libs.json.{JsObject, Json}

import java.time.{LocalDate, Month}

object PeriodDataBuilder {

  val aPeriodData: PeriodData = PeriodData(
    deductionPeriod = Month.MAY,
    deductionAmount = Some(100.00),
    costOfMaterials = Some(50.00),
    grossAmountPaid = Some(450.00),
    submissionDate = "2020-05-11T16:38:57.489Z",
    submissionId = Some("submissionId"),
    source = "customer"
  )

  def mapToJsonWrite(periodData: PeriodData)(implicit taxYear: Int): JsObject = {
    def yearFrom(taxYear: Int, month: Month): Int = if (month.getValue >= 4) taxYear else taxYear - 1

    val fromDate = LocalDate.of(yearFrom(taxYear, periodData.deductionPeriod), periodData.deductionPeriod.getValue - 1, 6)
    val toDate = LocalDate.of(yearFrom(taxYear, periodData.deductionPeriod), periodData.deductionPeriod.getValue, 5)

    Json.obj(fields =
      "deductionFromDate" -> fromDate.toString,
      "deductionToDate" -> toDate.toString,
      "deductionAmount" -> periodData.deductionAmount,
      "costOfMaterials" -> periodData.costOfMaterials,
      "grossAmountPaid" -> periodData.grossAmountPaid,
      "submissionDate" -> periodData.submissionDate,
      "submissionId" -> periodData.submissionId,
      "source" -> periodData.source
    )
  }

}
