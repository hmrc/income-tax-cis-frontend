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

import models.mongo.CisCYAModel
import play.api.libs.json.{Json, OWrites}

case class CreateNewCisContractor(contractorName: Option[String], ern: String, customerDeductionPeriod: DeductionPeriodData)

object CreateNewCisContractor {
  implicit def writes: OWrites[CreateNewCisContractor] = Json.writes[CreateNewCisContractor]

  def mapFrom(employerRef: String, cisCYAModel: CisCYAModel): Option[CreateNewCisContractor] = {
    cisCYAModel.periodData.map { periodData =>
      CreateNewCisContractor(
        contractorName = cisCYAModel.contractorName,
        ern = employerRef,
        customerDeductionPeriod = DeductionPeriodData(periodData)
      )
    }
  }
}
