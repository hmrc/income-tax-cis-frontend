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
import play.api.libs.json.OWrites
import utils.JsonUtils.jsonObjNoNulls

case class DeleteCisPeriodPayload(contractorDetails: ContractorDetails, periodData: PeriodData)

object DeleteCisPeriodPayload {

  implicit def writes: OWrites[DeleteCisPeriodPayload] = (payload: DeleteCisPeriodPayload) => {
    jsonObjNoNulls(
      "deletedCisPeriod" ->
        jsonObjNoNulls(
          "contractorDetails" -> payload.contractorDetails
        ).++(
          jsonObjNoNulls(
            "month" -> payload.periodData.deductionPeriod.toString,
            "labour" -> payload.periodData.grossAmountPaid,
            "cisDeduction" -> payload.periodData.deductionAmount,
            "paidForMaterials" -> payload.periodData.costOfMaterials.isDefined,
            "materialsCost" -> payload.periodData.costOfMaterials
          )
        )
    )
  }
}