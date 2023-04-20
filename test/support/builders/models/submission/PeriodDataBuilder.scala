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

package support.builders.models.submission

import models.submission.PeriodData
import support.TaxYearUtils.taxYearEOY

object PeriodDataBuilder {
  val aPeriodData: PeriodData = PeriodData(
    deductionFromDate = s"${taxYearEOY - 1}-04-06",
    deductionToDate = s"${taxYearEOY - 1}-05-05",
    grossAmountPaid = Some(500),
    deductionAmount = 100,
    costOfMaterials = Some(250)
  )
}
