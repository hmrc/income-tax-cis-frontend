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

import models.mongo.CisCYAModel
import support.builders.models.mongo.CYAPeriodDataBuilder.aCYAPeriodData

import java.time.Month.NOVEMBER

object CisCYAModelBuilder {

  val aCisCYAModel: CisCYAModel = CisCYAModel(
    contractorName = Some("ABC Steelworks"),
    periodData = Some(aCYAPeriodData),
    priorPeriodData = Seq(aCYAPeriodData.copy(deductionPeriod = NOVEMBER, originallySubmittedPeriod = Some(NOVEMBER)))
  )
}
