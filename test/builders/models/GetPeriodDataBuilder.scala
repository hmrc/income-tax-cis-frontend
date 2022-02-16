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

import models.GetPeriodData
import utils.{AUGUST, MAY}

object GetPeriodDataBuilder {

  val aGetPeriodData: GetPeriodData =
    GetPeriodData(
      deductionFromDate = MAY,
      deductionToDate = AUGUST,
      deductionAmount = Some(100.00),
      costOfMaterials = Some(50.00),
      grossAmountPaid = Some(450.00),
      submissionDate = "2020-05-11T16:38:57.489Z",
      submissionId = Some("submissionId"),
      source = "customer"
    )

}
