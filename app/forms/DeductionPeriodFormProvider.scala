/*
 * Copyright 2023 HM Revenue & Customs
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

import filters.InputFilters
import forms.FormTypes.DeductionPeriodForm
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.constraint
import models.forms.DeductionPeriod
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.Month
import javax.inject.Singleton
import scala.util.Try

@Singleton
class DeductionPeriodFormProvider extends InputFilters {

  def deductionPeriodForm(isAgent: Boolean,
                          unSubmittableMonths: Seq[Month] = Seq()): DeductionPeriodForm =
    Form(mapping(
      "month" -> trimmedText.transform[String](filter, identity)
        .verifying(validMonth(error(isAgent)))
        .verifying(alreadySubmittedCheck(unSubmittableMonths)(error(isAgent)))
    )(DeductionPeriod.formApply)(DeductionPeriod.formUnapply))

  private def error(isAgent: Boolean): String = s"deductionPeriod.error.${if (isAgent) "agent" else "individual"}"

  private def alreadySubmittedCheck(submittedMonths: Seq[Month]): String => Constraint[String] = msgKey => constraint[String] { month =>
    if (submittedMonths.map(_.toString).contains(month.toUpperCase)) Invalid(msgKey) else Valid
  }

  private def validMonth: String => Constraint[String] = msgKey => constraint[String] { month =>
    if (Try(Month.valueOf(month.toUpperCase)).toOption.isDefined) Valid else Invalid(msgKey)
  }
}
