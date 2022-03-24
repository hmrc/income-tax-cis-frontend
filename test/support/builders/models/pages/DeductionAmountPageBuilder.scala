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

package support.builders.models.pages

import forms.FormsProvider
import models.pages.DeductionAmountPage
import support.TaxYearHelper

import java.time.Month

object DeductionAmountPageBuilder extends TaxYearHelper {

  val aDeductionAmountPage: DeductionAmountPage = DeductionAmountPage(
    taxYear = taxYearEOY,
    month = Month.MAY,
    contractorName = Some("default-contractor"),
    employerRef = "default-employer-ref",
    form = new FormsProvider().deductionAmountForm(),
    originalAmount = None
  )
}