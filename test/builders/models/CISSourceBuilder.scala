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

import builders.models.CisDeductionsBuilder.aCisDeductions
import models.CISSource

object CISSourceBuilder {

  val aCISSource: CISSource =
    CISSource(
      totalDeductionAmount = Some(900.00),
      totalCostOfMaterials = Some(800.00),
      totalGrossAmountPaid = Some(700.00),
      cisDeductions = Seq(aCisDeductions)
    )
}