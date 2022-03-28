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

import forms.validation.StringConstraints.{validateChar, validateSize}
import play.api.data.Form
import play.api.data.Forms.mapping
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import models.pages.ContractorDetailsViewModel
import play.api.data.validation.Constraint
import play.api.data.validation.Constraints.nonEmpty


object ContractorDetailsForm {

  def contractorDetailsForm(
                            isAgent: Boolean
                           ): Form[ContractorDetailsViewModel] = {

    val contractorName = "contractorName"
    val employerReferenceNumber = "employerReferenceNumber"
    val nameCharLimit = 105
    val nameRegex = "^[A-Za-z0-9 \\-,.&';\\/]{1,105}$"
    val refRegex = "^[0-9]{3}\\/[^ ].{0,9}$"

    def nameNotEmpty(isAgent: Boolean): Constraint[String] =
      nonEmpty(s"contractor-details.name.error.noEntry.${if(isAgent) "agent" else "individual"}")

    val refNotEmpty: Constraint[String] =
      nonEmpty(s"contractor-details.employer-ref.error.noEntry")

    val nameNotCharLimit: Constraint[String] = validateSize(nameCharLimit)("contractor-details.name.error.notCharLimit")

    val validateNameFormat: Constraint[String] =
      validateChar(nameRegex)(s"contractor-details.name.error.wrongFormat")

    val validateRefFormat: Constraint[String] =
      validateChar(refRegex)(s"contractor-details.employer-ref.error.wrongFormat")

    Form(
      mapping(
        contractorName -> trimmedText.verifying(
          nameNotEmpty(isAgent) andThen nameNotCharLimit andThen validateNameFormat
        ),
        employerReferenceNumber -> trimmedText.verifying(
          refNotEmpty andThen validateRefFormat
        )
      )(ContractorDetailsViewModel.apply)(ContractorDetailsViewModel.unapply)
    )

  }

}
