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

package support.builders.models.nrs

import models.nrs.ViewCisPeriodPayload
import DeductionPeriodBuilder.aDeductionPeriod
import support.builders.models.nrs.ContractorDetailsBuilder.aContractorDetails

object ViewCisPeriodPayloadBuilder {
  val aViewCisPeriodPayload: ViewCisPeriodPayload = ViewCisPeriodPayload(aContractorDetails.copy(ern = "123/AB123456"), aDeductionPeriod)
}