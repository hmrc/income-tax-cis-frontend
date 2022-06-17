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

import models.mongo.CisUserData
import models.{CisDeductions, User}
import play.api.libs.json.{Json, OWrites}

import java.time.Month

case class ViewCisPeriodAudit(taxYear: Int,
                              userType: String,
                              nino: String,
                              mtditid: String,
                              cisPeriod: ContractorDetailsAndPeriodData) {

  private val name = "ViewCisPeriod"

  def toAuditModel: AuditModel[ViewCisPeriodAudit] = AuditModel(name, name, this)
}

object ViewCisPeriodAudit {

  implicit def writes: OWrites[ViewCisPeriodAudit] = Json.writes[ViewCisPeriodAudit]

  def mapFrom(taxYear: Int,
              user: User,
              deductions: CisDeductions,
              deductionMonth: Month
             ): Option[ViewCisPeriodAudit] =
    ContractorDetailsAndPeriodData.mapFrom(deductions, deductionMonth).map { cisPeriod =>
      ViewCisPeriodAudit(
        taxYear = taxYear,
        userType = user.affinityGroup.toLowerCase,
        nino = user.nino,
        mtditid = user.mtditid,
        cisPeriod = cisPeriod
      )
    }

  def mapFrom(taxYear: Int,
              user: User,
              cisUserData: CisUserData
             ): Option[ViewCisPeriodAudit] =
    ContractorDetailsAndPeriodData.mapFrom(cisUserData.employerRef, cisUserData.cis).map { cisPeriod =>
      ViewCisPeriodAudit(
        taxYear = taxYear,
        userType = user.affinityGroup.toLowerCase,
        nino = user.nino,
        mtditid = user.mtditid,
        cisPeriod = cisPeriod
      )
    }
}

