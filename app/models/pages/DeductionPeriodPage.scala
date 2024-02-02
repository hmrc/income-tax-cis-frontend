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

package models.pages

import forms.FormTypes.DeductionPeriodForm
import models.mongo.CisUserData

import java.time.Month

case class DeductionPeriodPage(taxYear: Int,
                               contractorName: Option[String],
                               employerRef: String,
                               period: Option[Month],
                               priorSubmittedPeriods: Seq[Month],
                               form: DeductionPeriodForm)

object DeductionPeriodPage {

  def apply(taxYear: Int,
            cisUserData: CisUserData,
            form: DeductionPeriodForm
           ): DeductionPeriodPage = DeductionPeriodPage(
    taxYear = taxYear,
    contractorName = cisUserData.cis.contractorName,
    employerRef = cisUserData.employerRef,
    period = cisUserData.cis.periodData.map(_.deductionPeriod),
    priorSubmittedPeriods = cisUserData.cis.priorPeriodData.map(_.deductionPeriod),
    form = form
  )
}
