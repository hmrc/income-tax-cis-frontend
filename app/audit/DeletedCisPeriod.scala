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

package audit

import models.PeriodData
import play.api.libs.json.{Json, OWrites}

case class DeletedCisPeriod(contractorDetails: ContractorNameAndEmployerRef,
                            month: String,
                            labour: Option[BigDecimal],
                            cisDeduction: Option[BigDecimal],
                            paidForMaterials: Boolean,
                            materialsCost: Option[BigDecimal])

object DeletedCisPeriod {
  implicit def writes: OWrites[DeletedCisPeriod] = Json.writes[DeletedCisPeriod]

  def apply(contractorName: Option[String], employerRef: String, periodData: PeriodData): DeletedCisPeriod =
    DeletedCisPeriod(
      contractorDetails = ContractorNameAndEmployerRef(name = contractorName, ern = employerRef),
      month = periodData.deductionPeriod.toString,
      labour = periodData.grossAmountPaid,
      cisDeduction = periodData.deductionAmount,
      paidForMaterials = periodData.costOfMaterials.isDefined,
      materialsCost = periodData.costOfMaterials
    )
}
