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

package support.builders.models.audit

import audit.AmendCisContractorAudit
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.AmendedCisContractorBuilder.anAmendedCisContractor
import support.builders.models.audit.PreviousCisContractorBuilder.aPreviousCisContractor
import support.TaxYearUtils.taxYearEOY

object AmendCisContractorAuditBuilder {
  val anAmendCisContractorAudit: AmendCisContractorAudit =
    AmendCisContractorAudit(
      taxYear = taxYearEOY,
      userType = aUser.affinityGroup.toLowerCase,
      nino = aUser.nino,
      mtditid = aUser.mtditid,
      previousContractor = aPreviousCisContractor,
      newContractor = anAmendedCisContractor
  )
}
