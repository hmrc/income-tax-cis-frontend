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

package models

import play.api.libs.json.{Json, OFormat}

case class CISSource(totalDeductionAmount: Option[BigDecimal],
                     totalCostOfMaterials: Option[BigDecimal],
                     totalGrossAmountPaid: Option[BigDecimal],
                     cisDeductions: Seq[CisDeductions]){

  def allEmployerRefs: Seq[String] = {
    cisDeductions.map(_.employerRef)
  }

  def cisDeductionsWith(employerRef: String): Option[CisDeductions] = cisDeductions.find(_.employerRef == employerRef)
}

object CISSource {
  implicit val format: OFormat[CISSource] = Json.format[CISSource]
}
