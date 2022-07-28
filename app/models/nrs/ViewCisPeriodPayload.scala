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

import play.api.libs.json.{Json, OWrites}
import utils.JsonUtils.jsonObjNoNulls

case class ViewCisPeriodPayload(name: Option[String],
                                ern: String,
                                month: String,
                                labour: Option[BigDecimal],
                                cisDeduction: Option[BigDecimal],
                                costOfMaterialsQuestion: Option[Boolean],
                                materialsCost: Option[BigDecimal])

object ViewCisPeriodPayload {
  implicit def writes: OWrites[ViewCisPeriodPayload] = (payload: ViewCisPeriodPayload) => {
    jsonObjNoNulls(
      "cisPeriod" -> jsonObjNoNulls(
        "name" -> payload.name,
        "ERN" -> payload.ern,
        "month" -> payload.month,
        "labour" -> payload.labour,
        "cisDeduction" -> payload.cisDeduction,
        "costOfMaterialsQuestion" -> payload.costOfMaterialsQuestion,
        "materialsCost" -> payload.materialsCost
      )
    )
  }
}
