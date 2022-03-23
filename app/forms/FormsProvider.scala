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

package forms

import play.api.data.Form

import javax.inject.Singleton

@Singleton
class FormsProvider {

  def labourPayAmountForm(isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"labourPayPage.error.noEntry.${if (isAgent) "agent" else "individual"}",
    wrongFormatKey = s"labourPayPage.error.wrongFormat.${if (isAgent) "agent" else "individual"}",
    exceedsMaxAmountKey = s"labourPayPage.error.overMaximum.${if (isAgent) "agent" else "individual"}"
  )

  def deductionAmountForm(): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = s"deductionAmountPage.error.noEntry",
    wrongFormatKey = s"deductionAmountPage.error.wrongFormat",
    exceedsMaxAmountKey = s"deductionAmountPage.error.overMaximum"
  )
}
