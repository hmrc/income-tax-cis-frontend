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

package audit

import models.mongo.CisCYAModel
import play.api.libs.json.{Json, OWrites}

case class AmendedCisContractor(contractorName: Option[String],
                         ern: String,
                         deductionPeriods: Seq[DeductionPeriodData] = Seq.empty,
                         customerDeductionPeriods: Option[DeductionPeriodData] = None)

object AmendedCisContractor {
  implicit def writes: OWrites[AmendedCisContractor] = Json.writes[AmendedCisContractor]

  def apply(employerRef: String,
            cisCYAModel: CisCYAModel): AmendedCisContractor = {
    AmendedCisContractor(
      contractorName = cisCYAModel.contractorName,
      ern = employerRef,
      deductionPeriods = cisCYAModel.priorPeriodData.map(DeductionPeriodData(_)),
      customerDeductionPeriods = cisCYAModel.periodData.map(DeductionPeriodData(_))
    )
  }
}
