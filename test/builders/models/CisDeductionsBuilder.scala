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

package builders.models

import builders.models.GetPeriodDataBuilder.aGetPeriodData
import models.CisDeductions

object CisDeductionsBuilder {

  val aCisDeductions: CisDeductions =
    CisDeductions(
      fromDate = "2020-05-05",
      toDate = "2020-06-06",
      contractorName = Some("ABC SteelWorks"),
      employerRef = "123/AB123456",
      totalDeductionAmount = Some(300.00),
      totalCostOfMaterials = Some(400.00),
      totalGrossAmountPaid = Some(200.00),
      periodData = Seq(aGetPeriodData)
    )
}
