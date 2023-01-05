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

package support.builders.models

import models.CisDeductions
import play.api.libs.json.{JsObject, Json}
import support.builders.models.PeriodDataBuilder.aPeriodData

object CisDeductionsBuilder {

  val aCisDeductions: CisDeductions = CisDeductions(
    fromDate = "2020-04-06",
    toDate = "2021-04-05",
    contractorName = Some("ABC Steelworks"),
    employerRef = "123/AB123456",
    totalDeductionAmount = Some(100.00),
    totalCostOfMaterials = Some(50.00),
    totalGrossAmountPaid = Some(450.00),
    periodData = Seq(aPeriodData)
  )

  def mapToJsonWrite(cisDeductions: CisDeductions)(implicit taxYear: Int): JsObject = Json.obj(fields =
    "fromDate" -> cisDeductions.fromDate,
    "toDate" -> cisDeductions.toDate,
    "contractorName" -> cisDeductions.contractorName,
    "employerRef" -> cisDeductions.employerRef,
    "totalDeductionAmount" -> cisDeductions.totalDeductionAmount,
    "totalCostOfMaterials" -> cisDeductions.totalCostOfMaterials,
    "totalGrossAmountPaid" -> cisDeductions.totalGrossAmountPaid,
    "periodData" -> cisDeductions.periodData.map(PeriodDataBuilder.mapToJsonWrite)
  )
}
