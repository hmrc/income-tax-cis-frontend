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

import models.User
import models.mongo.CisUserData
import play.api.libs.json.{Json, OWrites}

case class CreateNewCisContractorAudit(taxYear: Int,
                                       userType: String,
                                       nino: String,
                                       mtditid: String,
                                       contractor: CreateNewCisContractor) {

  private val name = "CreateNewCisContractor"

  def toAuditModel: AuditModel[CreateNewCisContractorAudit] = AuditModel(name, name, this)
}

object CreateNewCisContractorAudit {
  implicit def writes: OWrites[CreateNewCisContractorAudit] = Json.writes[CreateNewCisContractorAudit]

  def mapFrom(taxYear: Int, employerRef: String, user: User, cisUserData: CisUserData): Option[CreateNewCisContractorAudit] = {
    CreateNewCisContractor.mapFrom(employerRef = employerRef, cisCYAModel = cisUserData.cis).map { contractor =>
      CreateNewCisContractorAudit(
        taxYear = taxYear,
        userType = user.affinityGroup.toLowerCase,
        nino = user.nino,
        mtditid = user.mtditid,
        contractor = contractor
      )
    }
  }
}
