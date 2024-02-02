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

package models.nrs

import models.PeriodData
import models.mongo.CYAPeriodData
import play.api.libs.json.OWrites
import utils.JsonUtils.jsonObjNoNulls

case class ViewCisPeriodPayload(contractorDetails: ContractorDetails, customerDeductionPeriod: DeductionPeriod)

object ViewCisPeriodPayload {

  def apply(name: Option[String], employerRef: String, periodData: CYAPeriodData): ViewCisPeriodPayload = {
    ViewCisPeriodPayload(ContractorDetails(name, employerRef), DeductionPeriod(periodData))
  }

  def apply(name: Option[String], employerRef: String, periodData: PeriodData): ViewCisPeriodPayload = {
    ViewCisPeriodPayload(ContractorDetails(name, employerRef), DeductionPeriod(periodData))
  }

  implicit def writes: OWrites[ViewCisPeriodPayload] = (payload: ViewCisPeriodPayload) => {
    jsonObjNoNulls(
      "cisPeriod" -> jsonObjNoNulls(
        "name" -> payload.contractorDetails.name,
        "ERN" -> payload.contractorDetails.ern,
        "month" -> payload.customerDeductionPeriod.month,
        "labour" -> payload.customerDeductionPeriod.labour,
        "cisDeduction" -> payload.customerDeductionPeriod.cisDeduction,
        "costOfMaterialsQuestion" -> payload.customerDeductionPeriod.costOfMaterialsQuestion,
        "materialsCost" -> payload.customerDeductionPeriod.materialsCost
      )
    )
  }
}
