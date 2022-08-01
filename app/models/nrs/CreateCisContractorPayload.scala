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

import models.mongo.CYAPeriodData
import play.api.libs.json.OWrites
import utils.JsonUtils.jsonObjNoNulls

case class CreateCisContractorPayload(contractorDetails: ContractorDetails, customerDeductionPeriod: DeductionPeriod)

object CreateCisContractorPayload {

  def apply(contractorName: Option[String], employerRef: String, periodData: CYAPeriodData): CreateCisContractorPayload = {
    CreateCisContractorPayload(
      contractorDetails = ContractorDetails(name = contractorName, ern = employerRef),
      customerDeductionPeriod = DeductionPeriod(periodData)
    )
  }

  implicit def writes: OWrites[CreateCisContractorPayload] = (payload: CreateCisContractorPayload) => {
    jsonObjNoNulls(
      "contractor" -> jsonObjNoNulls(
        "contractorName" -> payload.contractorDetails.name,
        "ERN" -> payload.contractorDetails.ern,
        "customerDeductionPeriod" -> payload.customerDeductionPeriod
      )
    )
  }
}

