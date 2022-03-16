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

import java.time.Month

import filters.InputFilters
import forms.validation.mappings.MappingUtil.trimmedText
import javax.inject.Singleton
import models.forms.DeductionPeriod
import play.api.data.{Form, FormError}
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import forms.validation.utils.ConstraintUtil.constraint

import scala.util.Try

@Singleton
class DeductionPeriodFormProvider extends InputFilters {

  def error(isAgent: Boolean): String = s"deductionPeriod.error.${if (isAgent) "agent" else "individual"}"

  private def alreadySubmittedCheck(submittedMonths: Seq[Month]): String => Constraint[String] = msgKey => constraint[String](
    month => {
      if(submittedMonths.map(_.toString).contains(month.toUpperCase)) Invalid(msgKey) else Valid
    }
  )

  private def validMonth: String => Constraint[String] = msgKey => constraint[String](
    month => {
      if (Try(Month.valueOf(month.toUpperCase)).toOption.isDefined) Valid else Invalid(msgKey)
    }
  )

  def deductionPeriodForm(isAgent: Boolean, submittedMonths: Seq[Month] = Seq()): Form[DeductionPeriod] =
    Form(
      mapping(
        "month" -> trimmedText.transform[String](filter, x => x)
          .verifying(validMonth(error(isAgent)))
          .verifying(alreadySubmittedCheck(submittedMonths)(error(isAgent)))
      )(DeductionPeriod.formApply)(DeductionPeriod.formUnapply)
    )
}
