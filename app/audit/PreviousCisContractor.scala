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

package audit

import models.{AllCISDeductions, PeriodData}
import play.api.libs.json.{Json, OWrites}

case class PreviousCisContractor(contractorName: Option[String],
                                 ern: String,
                                 deductionPeriods: Seq[DeductionPeriodData] = Seq.empty,
                                 customerDeductionPeriods: Seq[DeductionPeriodData] = Seq.empty)

object PreviousCisContractor {

  implicit def writes: OWrites[PreviousCisContractor] = Json.writes[PreviousCisContractor]

  def apply(employerRef: String,
            optionalAllCISDeductions: Option[AllCISDeductions]): PreviousCisContractor = {
    optionalAllCISDeductions.map(item => mapFrom(employerRef, item))
      .getOrElse(PreviousCisContractor(contractorName = None, ern = employerRef))
  }

  private def mapFrom(employerRef: String, allCISDeductions: AllCISDeductions): PreviousCisContractor = {
    val deductionPeriods = allCISDeductions.contractorCisDeductionsWith(employerRef)
      .map(_.periodData)
      .map((item: Seq[PeriodData]) => item.map(DeductionPeriodData(_)))
      .getOrElse(Seq.empty)

    val customerDeductionPeriods = allCISDeductions.customerCisDeductionsWith(employerRef)
      .map(_.periodData)
      .map((item: Seq[PeriodData]) => item.map(DeductionPeriodData(_)))
      .getOrElse(Seq.empty)

    PreviousCisContractor(
      contractorName = allCISDeductions.eoyCisDeductionsWith(employerRef).flatMap(_.contractorName),
      ern = employerRef,
      deductionPeriods = deductionPeriods,
      customerDeductionPeriods = customerDeductionPeriods
    )
  }
}
