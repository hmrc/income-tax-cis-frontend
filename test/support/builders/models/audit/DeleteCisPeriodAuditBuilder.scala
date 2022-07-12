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

import audit.DeleteCisPeriodAudit
import support.builders.models.UserBuilder.aUser
import support.builders.models.audit.DeletedCisPeriodBuilder.aDeletedCisPeriod
import support.TaxYearUtils.taxYearEOY

object DeleteCisPeriodAuditBuilder {
  val aDeleteCisPeriodAudit: DeleteCisPeriodAudit =
    DeleteCisPeriodAudit(
      taxYear = taxYearEOY,
      userType = aUser.affinityGroup.toLowerCase,
      nino = aUser.nino,
      mtditid = aUser.mtditid,
      deletedCisPeriod = aDeletedCisPeriod
    )
}
