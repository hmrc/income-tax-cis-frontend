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


import models.mongo.{CYAPeriodData, CisCYAModel, CisUserData}
import models.{AllCISDeductions, CisDeductions, IncomeTaxUserData, PeriodData}
import play.api.libs.json.{Json, OWrites}

case class AmendCisContractorPayload(previousContractor: PreviousCisContractor, newContractor: NewCisContractor)

object AmendCisContractorPayload {
  implicit def writes: OWrites[AmendCisContractorPayload] = Json.writes[AmendCisContractorPayload]

  def apply(employerRef: String,
            cisUserData: CisUserData,
            incomeTaxUserData: IncomeTaxUserData): AmendCisContractorPayload = {

    val previousContractor: PreviousCisContractor = getPreviousContractors(employerRef, incomeTaxUserData.cis.get)
    val newContractor: NewCisContractor = getAmendCisContractor(employerRef, cisUserData.cis)

    AmendCisContractorPayload(previousContractor, newContractor)
  }

  private def getPreviousContractors(employerRef: String,
                                     allCISDeductions: AllCISDeductions): PreviousCisContractor = {

    val deductionPeriods = getDeductionPeriods(allCISDeductions.contractorCisDeductionsWith(employerRef))
    val customerDeductionPeriods = getDeductionPeriods(allCISDeductions.customerCisDeductionsWith(employerRef))

    PreviousCisContractor(contractorName = allCISDeductions.eoyCisDeductionsWith(employerRef).flatMap(_.contractorName).getOrElse(""),
      ern = employerRef, deductionPeriods, customerDeductionPeriods)
  }

  private def getAmendCisContractor(employerRef: String, cicCYAModel: CisCYAModel): NewCisContractor = {
    NewCisContractor(
      contractorName = cicCYAModel.contractorName.get,
      ern = employerRef,
      deductionPeriods = cicCYAModel.priorPeriodData.map(toDeductionPeriod),
      customerDeductionPeriods = cicCYAModel.periodData.map(toDeductionPeriod)
      )
  }

  private def toDeductionPeriod(periodData: CYAPeriodData): DeductionPeriod =
    DeductionPeriod(month = periodData.deductionPeriod.toString,
      labour = periodData.grossAmountPaid,
      cisDeduction = periodData.deductionAmount,
      costOfMaterialsQuestion = periodData.costOfMaterials.isDefined,
      materialsCost = periodData.costOfMaterials)

  private def getDeductionPeriods(cisDeductions: Option[CisDeductions]): Seq[DeductionPeriod] =
    cisDeductions.map(_.periodData)
      .map((item: Seq[PeriodData]) => item.map(periodData => DeductionPeriod(
        month = periodData.deductionPeriod.toString,
        labour = periodData.grossAmountPaid,
        cisDeduction = periodData.deductionAmount,
        costOfMaterialsQuestion = periodData.costOfMaterials.isDefined,
        materialsCost = periodData.costOfMaterials
      ))).getOrElse(Seq.empty)

}

