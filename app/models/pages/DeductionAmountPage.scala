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

import models.mongo.CisUserData
import play.api.data.Form

case class DeductionAmountPage(taxYear: Int,
                               month: Month,
                               contractorName: Option[String],
                               employerRef: String,
                               form: Form[BigDecimal],
                               originalAmount: Option[BigDecimal]) {

  val isReplay: Boolean = originalAmount.isDefined
}

object DeductionAmountPage {

  def apply(month: Month,
            cisUserData: CisUserData,
            form: Form[BigDecimal]): DeductionAmountPage = {
    val optAmount: Option[BigDecimal] = cisUserData.cis.periodData.flatMap(_.deductionAmount)

    DeductionAmountPage(
      taxYear = cisUserData.taxYear,
      month = month,
      contractorName = cisUserData.cis.contractorName,
      employerRef = cisUserData.employerRef,
      form = optAmount.fold(form)(amount => if (form.hasErrors) form else form.fill(amount)),
      originalAmount = optAmount
    )
  }
}
