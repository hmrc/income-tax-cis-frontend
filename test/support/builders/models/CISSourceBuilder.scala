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

package support.builders.models

import models.CISSource
import play.api.libs.json.{JsObject, Json}
import support.builders.models.CisDeductionsBuilder.aCisDeductions

object CISSourceBuilder {

  val aCISSource: CISSource = CISSource(
    totalDeductionAmount = Some(100.00),
    totalCostOfMaterials = Some(50.00),
    totalGrossAmountPaid = Some(450.00),
    cisDeductions = Seq(aCisDeductions)
  )

  def mapToJsonWrite(cisSource: CISSource)(implicit taxYear: Int): JsObject = Json.obj(fields =
    "totalDeductionAmount" -> cisSource.totalDeductionAmount,
    "totalCostOfMaterials" -> cisSource.totalCostOfMaterials,
    "totalGrossAmountPaid" -> cisSource.totalGrossAmountPaid,
    "cisDeductions" -> cisSource.cisDeductions.map(CisDeductionsBuilder.mapToJsonWrite)
  )
}
