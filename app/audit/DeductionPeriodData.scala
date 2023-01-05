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

package audit

import models.PeriodData
import models.mongo.CYAPeriodData
import play.api.libs.json.{Json, OWrites}

case class DeductionPeriodData(month: String,
                               labour: Option[BigDecimal],
                               cisDeduction: Option[BigDecimal],
                               paidForMaterials: Boolean,
                               materialsCost: Option[BigDecimal])

object DeductionPeriodData {
  implicit def writes: OWrites[DeductionPeriodData] = Json.writes[DeductionPeriodData]

  def apply(cyaPeriodData: CYAPeriodData): DeductionPeriodData = DeductionPeriodData(
    month = cyaPeriodData.deductionPeriod.toString,
    labour = cyaPeriodData.grossAmountPaid,
    cisDeduction = cyaPeriodData.deductionAmount,
    paidForMaterials = cyaPeriodData.costOfMaterialsQuestion.exists(identity),
    materialsCost = cyaPeriodData.costOfMaterials
  )

  def apply(periodData: PeriodData): DeductionPeriodData = DeductionPeriodData(
    month = periodData.deductionPeriod.toString,
    labour = periodData.grossAmountPaid,
    cisDeduction = periodData.deductionAmount,
    paidForMaterials = periodData.costOfMaterials.isDefined,
    materialsCost = periodData.costOfMaterials
  )
}
