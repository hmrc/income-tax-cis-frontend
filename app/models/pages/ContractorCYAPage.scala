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

package models.pages

import java.time.Month

import models.CisDeductions

case class ContractorCYAPage(taxYear: Int,
                             isInYear: Boolean,
                             contractorName: Option[String],
                             employerRef: String,
                             month: Month,
                             labourAmount: Option[BigDecimal],
                             deductionAmount: Option[BigDecimal],
                             costOfMaterials: Option[BigDecimal]) {

  val hasPaidForMaterials: Boolean = costOfMaterials.isDefined
}

object ContractorCYAPage {

  def mapToInYearPage(taxYear: Int,
                      cisDeductions: CisDeductions,
                      month: Month): ContractorCYAPage = {
    val periodData = cisDeductions.periodData.find(item => item.deductionPeriod == month).get

    ContractorCYAPage(
      taxYear = taxYear,
      isInYear = true,
      contractorName = cisDeductions.contractorName,
      employerRef = cisDeductions.employerRef,
      month = periodData.deductionPeriod,
      labourAmount = periodData.grossAmountPaid,
      deductionAmount = periodData.deductionAmount,
      costOfMaterials = periodData.costOfMaterials
    )
  }
}
