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

package audit

import models.mongo.CisUserData
import models.{IncomeTaxUserData, User}
import play.api.libs.json.{Json, OWrites}

case class AmendCisContractorAudit(taxYear: Int,
                                   userType: String,
                                   nino: String,
                                   mtditid: String,
                                   previousContractor: PreviousCisContractor,
                                   newContractor: AmendedCisContractor) {

  private val name = "AmendCisContractor"

  def toAuditModel: AuditModel[AmendCisContractorAudit] = AuditModel(name, name, this)
}

object AmendCisContractorAudit {
  implicit def writes: OWrites[AmendCisContractorAudit] = Json.writes[AmendCisContractorAudit]

  def apply(taxYear: Int,
            employerRef: String,
            user: User,
            cisUserData: CisUserData,
            incomeTaxUserData: IncomeTaxUserData
           ): AmendCisContractorAudit = {
    val previousContractor: PreviousCisContractor = PreviousCisContractor.apply(employerRef, incomeTaxUserData.cis)
    val newContractor: AmendedCisContractor = AmendedCisContractor(employerRef, cisUserData.cis)

    AmendCisContractorAudit(
      taxYear = taxYear,
      userType = user.affinityGroup.toLowerCase,
      nino = user.nino,
      mtditid = user.mtditid,
      previousContractor = previousContractor,
      newContractor = newContractor
    )
  }
}
