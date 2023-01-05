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

package models.nrs

import play.api.libs.json.OWrites
import utils.JsonUtils.jsonObjNoNulls

case class PreviousCisContractor(contractorName: String, ern: String, deductionPeriods: Seq[DeductionPeriod], customerDeductionPeriods: Seq[DeductionPeriod])

object PreviousCisContractor {
  implicit def writes: OWrites[PreviousCisContractor] = (previousCisContractor: PreviousCisContractor) => {
    jsonObjNoNulls(
      "contractorName" -> previousCisContractor.contractorName,
      "ERN" -> previousCisContractor.ern,
      "deductionPeriods" -> previousCisContractor.deductionPeriods,
      "customerDeductionPeriods" -> previousCisContractor.customerDeductionPeriods
    )
  }
}

