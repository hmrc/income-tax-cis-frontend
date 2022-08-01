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

package models.nrs

import models.PeriodData
import models.mongo.CYAPeriodData
import play.api.libs.json.{Json, OWrites}

case class DeductionPeriod(month: String,
                           labour: Option[BigDecimal],
                           cisDeduction: Option[BigDecimal],
                           costOfMaterialsQuestion: Boolean,
                           materialsCost: Option[BigDecimal])

object DeductionPeriod {
  implicit def writes: OWrites[DeductionPeriod] = Json.writes[DeductionPeriod]

  def apply(cyaPeriodData: CYAPeriodData): DeductionPeriod = DeductionPeriod(
    month = cyaPeriodData.deductionPeriod.toString,
    labour = cyaPeriodData.grossAmountPaid,
    cisDeduction = cyaPeriodData.deductionAmount,
    costOfMaterialsQuestion = cyaPeriodData.costOfMaterialsQuestion.exists(identity),
    materialsCost = cyaPeriodData.costOfMaterials
  )

  def apply(periodData: PeriodData): DeductionPeriod = DeductionPeriod(
    month = periodData.deductionPeriod.toString,
    labour = periodData.grossAmountPaid,
    cisDeduction = periodData.deductionAmount,
    costOfMaterialsQuestion = periodData.costOfMaterials.isDefined,
    materialsCost = periodData.costOfMaterials
  )
}
