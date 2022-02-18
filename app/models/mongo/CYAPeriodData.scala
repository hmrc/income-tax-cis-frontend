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

package models.mongo

import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import utils.EncryptedValue

import java.time.Month

case class CYAPeriodData(deductionPeriod: Month,
                         grossAmountPaid: Option[BigDecimal] = None,
                         deductionAmount: Option[BigDecimal] = None,
                         costOfMaterialsQuestion: Option[Boolean] = None,
                         costOfMaterials: Option[BigDecimal] = None) {

  def isFinished(): Boolean = {
    val costOfMaterialsFinished = costOfMaterialsQuestion match {
      case Some(true) => costOfMaterials.isDefined
      case Some(false) => true
      case None => false
    }

    grossAmountPaid.isDefined &&
      deductionAmount.isDefined &&
      costOfMaterialsFinished
  }
}

object CYAPeriodData {
  implicit val reads: Reads[Month] = JsPath.read[String].map(Month.valueOf)

  implicit val writes: Writes[Month] = Writes { month => JsString(month.toString) }

  implicit val format: OFormat[CYAPeriodData] = Json.format[CYAPeriodData]
}

case class EncryptedCYAPeriodData(deductionPeriod: EncryptedValue,
                                  grossAmountPaid: Option[EncryptedValue] = None,
                                  deductionAmount: Option[EncryptedValue] = None,
                                  costOfMaterialsQuestion: Option[EncryptedValue] = None,
                                  costOfMaterials: Option[EncryptedValue] = None)

object EncryptedCYAPeriodData extends MongoJodaFormats {

  implicit val mongoJodaDateTimeFormats: Format[DateTime] = dateTimeFormat

  implicit val formats: Format[EncryptedCYAPeriodData] = Json.format[EncryptedCYAPeriodData]
}

