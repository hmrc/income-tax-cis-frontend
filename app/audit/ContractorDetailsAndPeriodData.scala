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

package audit

import models.CisDeductions
import models.mongo.CisCYAModel
import play.api.libs.json.{Json, OWrites}

import java.time.Month

case class ContractorDetailsAndPeriodData(name: Option[String],
                                          ern: String,
                                          month: String,
                                          labour: Option[BigDecimal],
                                          cisDeduction: Option[BigDecimal],
                                          paidForMaterials: Boolean,
                                          materialsCost: Option[BigDecimal])

object ContractorDetailsAndPeriodData {

  implicit def writes: OWrites[ContractorDetailsAndPeriodData] = Json.writes[ContractorDetailsAndPeriodData]

  def mapFrom(deductions: CisDeductions, deductionMonth: Month): Option[ContractorDetailsAndPeriodData] =
    deductions.periodData.find(item => item.deductionPeriod == deductionMonth).map { periodData =>
      ContractorDetailsAndPeriodData.apply(
        name = deductions.contractorName,
        ern = deductions.employerRef,
        month = periodData.deductionPeriod.toString,
        labour = periodData.grossAmountPaid,
        cisDeduction = periodData.deductionAmount,
        paidForMaterials = periodData.costOfMaterials.isDefined,
        materialsCost = periodData.costOfMaterials
      )
    }

  def mapFrom(employerRef: String, cisCYAModel: CisCYAModel): Option[ContractorDetailsAndPeriodData] =
    cisCYAModel.periodData.map { periodData =>
      ContractorDetailsAndPeriodData.apply(
        name = cisCYAModel.contractorName,
        ern = employerRef,
        month = periodData.deductionPeriod.toString,
        labour = periodData.grossAmountPaid,
        cisDeduction = periodData.deductionAmount,
        paidForMaterials = periodData.costOfMaterials.isDefined,
        materialsCost = periodData.costOfMaterials
      )
    }
}
