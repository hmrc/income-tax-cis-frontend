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

package support.builders.models

import models.AllCISDeductions
import play.api.libs.json.{JsObject, Json}
import support.builders.models.CISSourceBuilder.aCISSource

object AllCISDeductionsBuilder {

  val anAllCISDeductions: AllCISDeductions = AllCISDeductions(
    customerCISDeductions = Some(aCISSource),
    contractorCISDeductions = Some(aCISSource)
  )

  def mapToJsonWrite(cis: AllCISDeductions)(implicit taxYear: Int): JsObject = Json.obj(fields =
    "customerCISDeductions" -> cis.customerCISDeductions.map(CISSourceBuilder.mapToJsonWrite),
    "contractorCISDeductions" -> cis.contractorCISDeductions.map(CISSourceBuilder.mapToJsonWrite)
  )
}
