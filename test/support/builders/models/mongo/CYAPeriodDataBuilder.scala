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

package support.builders.models.mongo

import models.mongo.CYAPeriodData

import java.time.Month

object CYAPeriodDataBuilder {

  val aCYAPeriodData: CYAPeriodData = CYAPeriodData(
    deductionPeriod = Month.MAY,
    grossAmountPaid = Some(500.00),
    deductionAmount = Some(100.00),
    costOfMaterialsQuestion = Some(true),
    costOfMaterials = Some(250.00),
    contractorSubmitted = false,
    originallySubmittedPeriod = Some(Month.MAY)
  )
}
